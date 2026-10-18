/*
 * Static checks for this RuneLite plugin: the Plugin Hub requirements and the rules from AGENTS.md
 * and docs/rules.md that can be caught by reading files. It can't prove a plugin is allowed. A clean
 * run only means none of the known red flags were found.
 *
 * Normally run through scripts/check.ps1 (Windows) or scripts/check.sh (Linux, macOS, cloud), which
 * also run the Gradle build. To run it on its own, from the repo root:
 *
 *     java scripts/Check.java            # placeholder names are warnings
 *     java scripts/Check.java --submit   # placeholder names are errors (use before a Plugin Hub PR)
 *
 * This is a dev tool, not plugin code: Gradle only compiles src/, so it is never part of the plugin.
 * It uses Java 11's single-file source launch, so it needs no build step of its own.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Check
{
	private static final Path ROOT = Paths.get("").toAbsolutePath();

	// Directories that hold build output or tool state, not source. Skipped when walking the repo.
	private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
		".git", ".gradle", "build", "bin", "out", ".idea", ".vscode", ".settings", "node_modules"));

	// Keys runelite-plugin.properties may contain. Any other key fails the Plugin Hub build.
	private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
		"displayName", "author", "description", "tags", "version", "plugins", "build"));
	private static final List<String> REQUIRED_KEYS = Arrays.asList(
		"displayName", "author", "description", "plugins");

	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

	private static final class Rule
	{
		final boolean error;
		final Pattern pattern;
		final String message;

		Rule(boolean error, String regex, String message)
		{
			this.error = error;
			this.pattern = Pattern.compile(regex);
			this.message = message;
		}
	}

	// Matched line by line against src/main/java, after comments and string literals are blanked out.
	private static final List<Rule> CODE_RULES = Arrays.asList(
		// Forbidden outright (docs/rules.md, "Forbidden Language Features" and "Code").
		new Rule(true, "\\bjava\\.lang\\.reflect\\b|\\.getDeclared(Field|Method|Constructor)s?\\s*\\(|\\.setAccessible\\s*\\(|\\bClass\\s*\\.\\s*forName\\s*\\(",
			"Reflection is forbidden."),
		new Rule(true, "\\bjava\\.lang\\.invoke\\b|\\b(URL)?ClassLoader\\b|\\bdefineClass\\s*\\(|\\bjavax\\.tools\\b|\\bScriptEngine",
			"No dynamic code loading or runtime code generation."),
		new Rule(true, "\\bProcessBuilder\\b|\\bRuntime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*exec\\b",
			"No external processes."),
		new Rule(true, "\\bsun\\.misc\\.Unsafe\\b|\\bcom\\.sun\\.jna\\b|\\borg\\.lwjgl\\b|\\bSystem\\s*\\.\\s*load(Library)?\\s*\\(|\\bnative\\s+[\\w<>\\[\\]]+\\s+\\w+\\s*\\(",
			"No JNI, JNA, Unsafe or native code."),
		new Rule(true, "\\bObject(Input|Output)Stream\\b",
			"No Java (de)serialization. Use the injected Gson for data."),
		new Rule(true, "\\bHttpURLConnection\\b|\\bjava\\.net\\.http\\b|\\borg\\.apache\\.http\\b|\\.openConnection\\s*\\(|\\.openStream\\s*\\(",
			"Use the injected OkHttpClient for HTTP."),
		new Rule(true, "\\bnew\\s+OkHttpClient\\b",
			"@Inject OkHttpClient instead of creating one (use .newBuilder() to customise it)."),
		new Rule(true, "\\bnew\\s+Gson(Builder)?\\s*\\(",
			"@Inject Gson instead of creating one (use gson.newBuilder() to customise it)."),
		new Rule(true, "\\bThread\\s*\\.\\s*sleep\\s*\\(",
			"Never use Thread.sleep()."),
		new Rule(true, "\\bjava\\.awt\\.Desktop\\b|\\bDesktop\\s*\\.\\s*getDesktop\\b",
			"Use LinkBrowser to open URLs, not java.awt.Desktop."),
		new Rule(true, "\\bJFileChooser\\b",
			"Use Filepath.Chooser instead of JFileChooser."),
		new Rule(true, "\\bjava\\.awt\\.Robot\\b|\\bnew\\s+(KeyEvent|MouseEvent|MouseWheelEvent)\\s*\\(|\\.dispatchEvent\\s*\\(",
			"No injected input events (automation)."),
		new Rule(true, "\\bmenuAction\\s*\\(",
			"No menuAction: sending actions to the server is automation."),
		new Rule(true, "\\bgetWidget\\s*\\(\\s*\\d",
			"Raw widget ID. Use a net.runelite.api.gameval.InterfaceID constant."),

		// Allowed, but a reviewer will look closely, or it goes against the project's low-maintenance goals.
		new Rule(false, "\\bnew\\s+(java\\.io\\.)?(File|FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)\\s*\\(|\\bjava\\.nio\\.file\\b",
			"File access must go through Filepath (which needs PluginDescriptor internalName). Check that this does."),
		new Rule(false, "\\bCHATBOX_TYPED_TEXT\\b",
			"Writing to the chatbox input is autotyping, which is forbidden. Reading it is fine."),
		new Rule(false, "\\bawaitTermination\\s*\\(",
			"Don't block in startUp()/shutDown(). Use shutdownNow()."),
		new Rule(false, "\\.execute\\s*\\(\\s*\\)",
			"Synchronous OkHttp call? Use enqueue(), and never call OkHttp on the client thread."),
		new Rule(false, "\\b(MenuEntryAdded|MenuOptionClicked|MenuOpened)\\b|\\bsetMenuEntries\\s*\\(|\\bcreateMenuEntry\\s*\\(",
			"Menu changes are rules-sensitive. Check docs/rules.md (Menus)."),
		new Rule(false, "\\bChatMessage\\b",
			"Matching chat text is fragile. Prefer a varbit/varp if one exists (Var Inspector)."),
		new Rule(false, "\\bgetScene\\s*\\(|\\bgetTiles\\s*\\(|\\bgetNpcs\\s*\\(|\\bgetPlayers\\s*\\(",
			"Scene scanning. Track spawn/despawn events in your own collection instead."),
		new Rule(false, "\\bgetVarbitValue\\s*\\(\\s*\\d|\\bgetVarpValue\\s*\\(\\s*\\d|\\bgetVar(bit|p)Id\\s*\\(\\s*\\)\\s*[!=]=\\s*\\d",
			"Raw var ID. Use a VarbitID / VarPlayerID constant from net.runelite.api.gameval."),
		new Rule(false, "\\bWidgetInfo\\b",
			"WidgetInfo is deprecated. Use net.runelite.api.gameval.InterfaceID."),
		new Rule(false, "\\bnet\\.runelite\\.api\\.(ItemID|ObjectID|NullObjectID|NpcID|AnimationID|Varbits|VarPlayer)\\b",
			"Old ID class. Use the net.runelite.api.gameval equivalent."),
		new Rule(false, "\\bSystem\\s*\\.\\s*(out|err)\\b|\\.printStackTrace\\s*\\(",
			"Use the Lombok @Slf4j log (log.debug for diagnostics) instead of printing.")
	);

	private static final List<String> TEMPLATE_LEFTOVERS = Arrays.asList("com.example", "ExamplePlugin", "ExampleConfig");

	private static boolean submit;
	private static int errors;
	private static int warnings;

	public static void main(String[] args) throws IOException
	{
		for (String arg : args)
		{
			if (arg.equals("--submit"))
			{
				submit = true;
			}
			else
			{
				System.err.println("Unknown argument: " + arg);
				System.err.println("Usage: java scripts/Check.java [--submit]");
				System.exit(2);
			}
		}

		if (!Files.isRegularFile(ROOT.resolve("runelite-plugin.properties")))
		{
			System.err.println("Run this from the repo root (no runelite-plugin.properties in " + ROOT + ").");
			System.exit(2);
		}

		System.out.println("==> Static checks" + (submit ? " (--submit: placeholder names are errors)" : ""));

		checkProperties();
		checkBuildFiles();
		checkRepoFiles();
		checkIcon();
		checkJavaSources();

		System.out.println();
		System.out.println(errors + " error(s), " + warnings + " warning(s).");
		if (errors == 0 && warnings > 0)
		{
			System.out.println("Warnings don't fail the check. Read each one and make sure it is intended.");
		}
		System.exit(errors > 0 ? 1 : 0);
	}

	// --- runelite-plugin.properties ---------------------------------------------------------------

	private static void checkProperties() throws IOException
	{
		String file = "runelite-plugin.properties";
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(ROOT.resolve(file)))
		{
			props.load(in);
		}

		for (String key : props.stringPropertyNames())
		{
			if (!ALLOWED_KEYS.contains(key))
			{
				error(file, 0, "Unknown key '" + key + "'. Any key besides " + ALLOWED_KEYS + " fails the Plugin Hub build.");
			}
		}
		for (String key : REQUIRED_KEYS)
		{
			if (props.getProperty(key, "").trim().isEmpty())
			{
				error(file, 0, "Required key '" + key + "' is missing or empty.");
			}
		}

		// The example-plugin template's values.
		String displayName = props.getProperty("displayName", "").trim();
		String author = props.getProperty("author", "").trim();
		String description = props.getProperty("description", "").trim();
		if (displayName.equals("Example") || author.equals("Nobody") || description.equals("An example greeter plugin"))
		{
			error(file, 0, "Still has the example-plugin template's values (displayName/author/description).");
		}

		String build = props.getProperty("build", "").trim();
		if (!build.equals("standard"))
		{
			error(file, 0, "build=" + build + ". Keep build=standard (no third-party dependencies).");
		}

		for (String className : props.getProperty("plugins", "").split(","))
		{
			className = className.trim();
			if (className.isEmpty())
			{
				continue;
			}
			Path source = ROOT.resolve("src/main/java/" + className.replace('.', '/') + ".java");
			if (!Files.isRegularFile(source))
			{
				error(file, 0, "plugins=" + className + ", but " + rel(source) + " doesn't exist.");
			}
		}

		// Our own placeholders (CLAUDE.md, "Renaming once an idea is picked").
		if (displayName.contains("WIP") || displayName.contains("Starter") || description.contains("Feature to be decided"))
		{
			placeholder(file, 0, "displayName/description are still the placeholder values.");
		}
		if (props.getProperty("plugins", "").contains("com.alexm15.starter"))
		{
			placeholder(file, 0, "plugins still points at the placeholder package com.alexm15.starter.");
		}
	}

	// --- build.gradle / settings.gradle -----------------------------------------------------------

	private static void checkBuildFiles() throws IOException
	{
		String gradle = read(ROOT.resolve("build.gradle"));
		if (!gradle.contains("options.release.set(11)"))
		{
			error("build.gradle", 0, "Must compile with options.release.set(11) (Plugin Hub requires Java 11).");
		}
		forEachMatch("build.gradle", gradle, Pattern.compile("com\\.google\\.code\\.gson|com\\.google\\.inject|com\\.squareup\\.okhttp"), true,
			"Don't add RuneLite's transitive dependencies (Gson, Guice, OkHttp) to build.gradle.");
		forEachMatch("build.gradle", gradle, Pattern.compile("com\\.example"), true,
			"Template leftover: rename com.example.");

		Path settings = ROOT.resolve("settings.gradle");
		if (Files.isRegularFile(settings))
		{
			forEachMatch("settings.gradle", read(settings), Pattern.compile("rootProject\\.name\\s*=\\s*['\"]example['\"]"), true,
				"Template leftover: rename the project from 'example'.");
		}
	}

	// --- Whole-repo file checks -------------------------------------------------------------------

	private static void checkRepoFiles() throws IOException
	{
		if (!Files.isRegularFile(ROOT.resolve("LICENSE")))
		{
			error("LICENSE", 0, "Missing. The Plugin Hub needs an open-source license file (BSD-2-Clause here).");
		}
		for (String dir : Arrays.asList("out", ".tmp"))
		{
			if (Files.isDirectory(ROOT.resolve(dir)))
			{
				warn(dir + "/", 0, "Build output folder. Make sure it is never committed.");
			}
		}

		for (Path path : walk(ROOT))
		{
			String name = path.getFileName().toString();
			String rel = rel(path);
			if (name.equals("credentials.properties"))
			{
				error(rel, 0, "RuneLite credentials file inside the repo. Delete it; it logs into your account without a password.");
			}
			else if (name.endsWith(".class"))
			{
				error(rel, 0, "Compiled .class file outside build/. Don't commit build artifacts.");
			}
			else if (name.endsWith(".jar") && !rel.equals("gradle/wrapper/gradle-wrapper.jar"))
			{
				error(rel, 0, "Jar file in the repo. Don't commit build artifacts or bundled libraries.");
			}
			else if (rel.endsWith("META-INF/services/net.runelite.client.plugins.Plugin"))
			{
				error(rel, 0, "Remove this file; the Plugin Hub rejects it.");
			}
			else if (name.toLowerCase().endsWith(".png") && rel.startsWith("src/"))
			{
				int[] size = pngSize(path);
				if (size == null)
				{
					error(rel, 0, "Not a real PNG (wrong file signature). Re-export it as PNG; don't just rename it.");
				}
				else if ((long) size[0] * size[1] * 4 > 1024 * 1024)
				{
					warn(rel, 0, size[0] + "x" + size[1] + " px takes " + (size[0] * size[1] * 4 / 1024)
						+ " KiB in memory once loaded. Shrink it if you can.");
				}
			}
		}
	}

	private static void checkIcon() throws IOException
	{
		Path icon = ROOT.resolve("icon.png");
		if (!Files.isRegularFile(icon))
		{
			return; // Optional.
		}
		int[] size = pngSize(icon);
		if (size == null)
		{
			error("icon.png", 0, "Not a real PNG (wrong file signature).");
			return;
		}
		if ((long) size[0] * size[1] > 5000)
		{
			error("icon.png", 0, size[0] + "x" + size[1] + " = " + (size[0] * size[1]) + " px². The maximum is 5000 px² (about 48x72 is recommended).");
		}
		if (Files.size(icon) > 256 * 1024)
		{
			error("icon.png", 0, (Files.size(icon) / 1024) + " KiB. The maximum is 256 KiB.");
		}
	}

	// --- Java sources -----------------------------------------------------------------------------

	private static void checkJavaSources() throws IOException
	{
		for (Path path : walk(ROOT.resolve("src")))
		{
			if (!path.toString().endsWith(".java"))
			{
				continue;
			}
			String rel = rel(path);
			String source = read(path);
			String code = blankCommentsAndStrings(source);

			for (String leftover : TEMPLATE_LEFTOVERS)
			{
				forEachMatch(rel, code, Pattern.compile("\\b" + Pattern.quote(leftover) + "\\b"), true,
					"Template leftover: rename " + leftover + ".");
			}
			// Checked on the raw source, since the group name is a string literal.
			forEachMatch(rel, source, Pattern.compile("@ConfigGroup\\s*\\(\\s*\"example\"\\s*\\)"), true,
				"Template leftover: config group \"example\" is too generic. Use a specific group name.");
			if (code.contains("com.alexm15.starter"))
			{
				placeholder(rel, 0, "Still in the placeholder package com.alexm15.starter.");
			}

			// The rule checks apply to shipped code only. Test code (like the dev-client launcher) isn't in the plugin.
			if (!rel.startsWith("src/main/"))
			{
				continue;
			}
			String[] lines = code.split("\n", -1);
			for (int i = 0; i < lines.length; i++)
			{
				for (Rule rule : CODE_RULES)
				{
					if (rule.pattern.matcher(lines[i]).find())
					{
						if (rule.error)
						{
							error(rel, i + 1, rule.message);
						}
						else
						{
							warn(rel, i + 1, rule.message);
						}
					}
				}
			}
		}
	}

	/**
	 * Replaces comments and the contents of string and char literals with spaces, keeping newlines and the
	 * quote characters, so line numbers still match and a rule never fires on a comment or a message.
	 */
	private static String blankCommentsAndStrings(String src)
	{
		StringBuilder out = new StringBuilder(src.length());
		int i = 0;
		int n = src.length();
		while (i < n)
		{
			char c = src.charAt(i);
			char next = i + 1 < n ? src.charAt(i + 1) : '\0';
			if (c == '/' && next == '/')
			{
				while (i < n && src.charAt(i) != '\n')
				{
					out.append(' ');
					i++;
				}
			}
			else if (c == '/' && next == '*')
			{
				out.append("  ");
				i += 2;
				while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/'))
				{
					out.append(src.charAt(i) == '\n' ? '\n' : ' ');
					i++;
				}
				if (i < n)
				{
					out.append("  ");
					i += 2;
				}
			}
			else if (c == '"' || c == '\'')
			{
				out.append(c);
				i++;
				while (i < n && src.charAt(i) != c && src.charAt(i) != '\n')
				{
					if (src.charAt(i) == '\\' && i + 1 < n)
					{
						out.append("  ");
						i += 2;
					}
					else
					{
						out.append(' ');
						i++;
					}
				}
				if (i < n && src.charAt(i) == c)
				{
					out.append(c);
					i++;
				}
			}
			else
			{
				out.append(c);
				i++;
			}
		}
		return out.toString();
	}

	// --- Helpers ----------------------------------------------------------------------------------

	private static void forEachMatch(String file, String text, Pattern pattern, boolean isError, String message)
	{
		String[] lines = text.split("\n", -1);
		for (int i = 0; i < lines.length; i++)
		{
			Matcher m = pattern.matcher(lines[i]);
			if (m.find())
			{
				if (isError)
				{
					error(file, i + 1, message);
				}
				else
				{
					warn(file, i + 1, message);
				}
			}
		}
	}

	/** Returns {width, height} from the PNG header, or null if the file isn't a PNG. */
	private static int[] pngSize(Path path) throws IOException
	{
		byte[] header = new byte[24];
		try (InputStream in = Files.newInputStream(path))
		{
			if (in.readNBytes(header, 0, header.length) < header.length)
			{
				return null;
			}
		}
		for (int i = 0; i < PNG_SIGNATURE.length; i++)
		{
			if (header[i] != PNG_SIGNATURE[i])
			{
				return null;
			}
		}
		// The first chunk is always IHDR: width and height are big-endian ints at bytes 16 and 20.
		return new int[]{readInt(header, 16), readInt(header, 20)};
	}

	private static int readInt(byte[] b, int offset)
	{
		return ((b[offset] & 0xff) << 24) | ((b[offset + 1] & 0xff) << 16) | ((b[offset + 2] & 0xff) << 8) | (b[offset + 3] & 0xff);
	}

	private static List<Path> walk(Path start) throws IOException
	{
		if (!Files.isDirectory(start))
		{
			return new ArrayList<>();
		}
		try (Stream<Path> stream = Files.walk(start))
		{
			return stream
				.filter(Files::isRegularFile)
				.filter(p -> !inSkippedDir(p))
				.sorted()
				.collect(Collectors.toList());
		}
	}

	private static boolean inSkippedDir(Path path)
	{
		Path relative = ROOT.relativize(path);
		for (int i = 0; i < relative.getNameCount() - 1; i++)
		{
			if (SKIP_DIRS.contains(relative.getName(i).toString()))
			{
				return true;
			}
		}
		return false;
	}

	private static String read(Path path) throws IOException
	{
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
	}

	private static String rel(Path path)
	{
		return ROOT.relativize(path.toAbsolutePath()).toString().replace('\\', '/');
	}

	private static void placeholder(String file, int line, String message)
	{
		if (submit)
		{
			error(file, line, message + " Rename before submitting (CLAUDE.md, \"Renaming once an idea is picked\").");
		}
		else
		{
			warn(file, line, message);
		}
	}

	private static void error(String file, int line, String message)
	{
		errors++;
		report("ERROR", file, line, message);
	}

	private static void warn(String file, int line, String message)
	{
		warnings++;
		report("WARN ", file, line, message);
	}

	private static void report(String level, String file, int line, String message)
	{
		System.out.println("  " + level + " " + file + (line > 0 ? ":" + line : "") + "  " + message);
	}
}
