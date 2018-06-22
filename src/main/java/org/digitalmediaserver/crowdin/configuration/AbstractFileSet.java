/*
 * Crowdin Maven Plugin, an Apache Maven plugin for synchronizing translation
 * files using the crowdin.com API.
 * Copyright (C) 2018 Digital Media Server developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.digitalmediaserver.crowdin.configuration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * An abstract {@link org.apache.maven.plugin.Mojo} configuration class
 * describing a set of files.
 *
 * @author Nadahar
 */
public abstract class AbstractFileSet {

	/**
	 * The encoding to use when deploying the translation files. This can either
	 * be a valid {@link Charset} name, or the special value
	 * {@code "Properties"}. Encoding is often determined by {@link #type} and
	 * only need to be explicitly set to deviate from the default.
	 * <p>
	 * The {@code "Properties"} maps to {@link StandardCharsets#ISO_8859_1}.
	 * Together with {@link #escapeUnicode} == {@code true}, any characters that
	 * don't exist in ISO 8859-1 will be encoded as &#92;u{@code <xxxx>} where
	 * {@code <xxxx>} is the hexadecimal Unicode value.
	 *
	 * @parameter
	 */
	@Nullable
	protected String encoding;

	/**
	 * Whether or not the language strings should be sorted by their key in the
	 * translation files when exporting them from crowdin. Mostly useful for
	 * {@link Properties} files. Defaults to {@code true} if {@code encoding} is
	 * {@code "Properties"}, {@code false} otherwise.
	 *
	 * @parameter
	 */
	@Nullable
	protected Boolean sortLines;

	/**
	 * Whether or not to add a comment header to the translation files when
	 * exporting them from crowdin. If no custom comment is provided, a generic
	 * "do not modify" comment will be added.
	 *
	 * @parameter default-value="true"
	 */
	@Nullable
	protected Boolean addComent;

	/**
	 * The custom comment header to add to translation files when exporting them
	 * from crowdin if {@link #addComent} is {@code true}. If not configured, a
	 * generic "do not modify" comment will be added.
	 *
	 * @parameter
	 */
	@Nullable
	protected String comment;

	/**
	 * The string to use as line separator when exporting files from crowdin.
	 * Specify \n, \r or \r\n as needed. If not specified, the default will be
	 * used.
	 *
	 * @parameter
	 */
	@Nullable
	protected String lineSeparator;

	/**
	 * Whether or not to encode Unicode characters in the form "&#92;uxxxx" when
	 * exporting files from crowdin. This setting only applies to
	 * {@link FileType#properties} file sets.
	 *
	 * @parameter default-value="true"
	 */
	@Nullable
	protected Boolean escapeUnicode;

	/**
	 * The {@link FileType} for this fileset. If not specified,
	 * auto-detection will be attempted with fall-back to
	 * {@link FileType#auto}.
	 *
	 * @parameter
	 */
	@Nullable
	protected FileType type;

	/**
	 * A list of {@link PlaceholderConversion} elements to apply to the
	 * translation file names.
	 *
	 * @parameter
	 */
	@Nullable
	protected List<PlaceholderConversion> placeholderConversions;

	/**
	 * Paths to include using a basic filter where {@code ?} and {@code *} are
	 * wildcards and the rest are literals. If one or more inclusions are
	 * configured the file set becomes a white-list where anything not included
	 * is excluded.
	 *
	 * @parameter
	 */
	@Nullable
	protected List<String> includes;

	/**
	 * Paths to exclude using a basic filter where {@code ?} and {@code *} are
	 * wildcards and the rest are literals.
	 *
	 * @parameter
	 */
	@Nullable
	protected List<String> excludes;

	/**
	 * For internal use.
	 */
	@Nullable
	protected Charset charset;

	/**
	 * @return The character encoding to convert translation files to when
	 *         exporting them from crowdin or {@code null} if not set.
	 */
	@Nullable
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding the character encoding to convert translation files to
	 *            when exporting them from crowdin or {@code null} to use the
	 *            default.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * @return The Charset to convert translation files to when exporting them
	 *         from crowdin or {@code null} to use the default.
	 */
	@Nullable
	public Charset getCharset() {
		return charset;
	}

	/**
	 * @param charset the {@link Charset} to convert translation files to when
	 *            exporting them from crowdin or {@code null} to use the
	 *            default.
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	/**
	 * @return {@code true} if lines should be sorted when exporting files from
	 *         crowdin, {@code false} otherwise.
	 */
	@Nullable
	public Boolean getSortLines() {
		return sortLines;
	}

	/**
	 * @return {@code true} if a comment should be added at the top of the
	 *         translated files when exporting them from crowdin, {@code false}
	 *         if it should not or {@code null} if not specified.
	 */
	@Nullable
	public Boolean getAddComent() {
		return addComent;
	}

	/**
	 * @return The custom comment header to add to translation files when
	 *         exporting them from crowdin, or {@code null} if the default
	 *         should be used.
	 */
	@Nullable
	public String getComment() {
		return comment;
	}

	/**
	 * @return The {@link String} to use as line separator when exporting files
	 *         from crowdin or {@code null} to use the default.
	 */
	@Nullable
	public String getLineSeparator() {
		return lineSeparator;
	}

	/**
	 * @return Whether to escape Unicode characters with "&#92;uxxxx" in
	 *         {@link FileType#properties} files when exporting them from
	 *         crowdin, or {@code null} if not set.
	 */
	@Nullable
	public Boolean getEscapeUnicode() {
		return escapeUnicode;
	}

	/**
	 * @return The {@link FileType}.
	 */
	@Nullable
	public FileType getType() {
		return type;
	}

	/**
	 * @param type the {@link FileType} to set.
	 */
	public void setType(@Nullable FileType type) {
		this.type = type;
	}

	/**
	 * @return The {@link List} of {@link PlaceholderConversion}s.
	 */
	@Nullable
	public List<PlaceholderConversion> getPlaceholderConversions() {
		return placeholderConversions;
	}

	/**
	 * @return The {@link List} of string patterns for paths to include. The
	 *         patterns use a basic filter where {@code ?} and {@code *} are
	 *         wildcards and the rest are literals. If one or more inclusions
	 *         are configured the file set becomes a white-list where anything
	 *         not included is excluded.
	 */
	@Nullable
	public List<String> getIncludes() {
		return includes;
	}

	/**
	 * @return The {@link List} of string patterns for paths to exclude. The
	 *         patterns use a basic filter where {@code ?} and {@code *} are
	 *         wildcards and the rest are literals.
	 */
	@Nullable
	public List<String> getExcludes() {
		return excludes;
	}

	/**
	 * Since the constructor is called automagically by Maven, verification and
	 * initialization of defaults is done here.
	 *
	 * @param fileSets the {@link List} of file sets to initialize.
	 * @throws MojoExecutionException If the initialization fails.
	 */
	public static void initialize(@Nullable List<? extends AbstractFileSet> fileSets) throws MojoExecutionException {
		if (fileSets == null || fileSets.isEmpty()) {
			return;
		}
		for (AbstractFileSet fileSet : fileSets) {
			fileSet.initializeInstance();
		}
	}

	/**
	 * Since the constructor is called automagically by Maven, verification and
	 * initialization of defaults is done here.
	 *
	 * @throws MojoExecutionException If the initialization fails.
	 */
	protected void initializeInstance() throws MojoExecutionException {
		if (placeholderConversions != null && !placeholderConversions.isEmpty()) {
			for (PlaceholderConversion conversion : placeholderConversions) {
				if (conversion.getFrom() == null || conversion.getFrom().isEmpty() || conversion.getTo() == null) {
					String from = conversion.getFrom() == null ? "null" : "\"" + conversion.getFrom() + "\"";
					String to = conversion.getTo() == null ? "null" : "\"" + conversion.getTo() + "\"";
					throw new MojoExecutionException(
						"Invalid placeholderConversion: \"" + from + " -> " + to + "\" in file set \"" + toString() + "\""
					);
				}
			}
		}
	}
}
