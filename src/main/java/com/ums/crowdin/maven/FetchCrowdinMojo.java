package com.ums.crowdin.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.jdom.Document;
import org.jdom.Element;
import com.ums.crowdin.maven.tool.CodeConversion;
import com.ums.crowdin.maven.tool.SortedProperties;
import com.ums.crowdin.maven.tool.SpecialArtifact;
import com.ums.crowdin.maven.tool.TranslationFile;

/**
 *Fetch crowdin translations in this project, looking dependencies
 *
 * @goal fetch
 * @threadSafe
 */
public class FetchCrowdinMojo extends AbstractCrowdinMojo {

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private DependencyTreeBuilder treeBuilder;

	/** parameter default-value="${localRepository}" */
	private ArtifactRepository localRepository;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactMetadataSource artifactMetadataSource;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactCollector artifactCollector;

	public final String statusFileName = "languages.properties";

	private void cleanFolders(Set<TranslationFile> translationFiles) {
		if (downloadFolder.exists()) {
			File[] languageFolders = downloadFolder.listFiles();
			for (File languageFolder : languageFolders) {
				if (!languageFolder.getName().startsWith(".") && languageFolder.isDirectory()) {
					if (!containsLanguage(translationFiles, languageFolder.getName())) {
						deleteFolder(languageFolder, true);
					} else {
						cleanLanguageFolder(languageFolder, translationFiles);
					}
				}
			}
		}
	}

	private void cleanLanguageFolder(File languageFolder, Set<TranslationFile> translationFiles) {
		File[] mavenIds = languageFolder.listFiles();
		for (File mavenId : mavenIds) {
			if (!mavenId.getName().startsWith(".") && mavenId.isDirectory()) {
				if (!containsMavenId(translationFiles, mavenId.getName())) {
					deleteFolder(mavenId, true);
				} else {
					deleteFolder(mavenId, false);
				}
			}
		}
	}

	private boolean containsLanguage(Set<TranslationFile> translationFiles, String language) {
		for (TranslationFile translationFile : translationFiles) {
			if (translationFile.getLanguage().equals(language)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsMavenId(Set<TranslationFile> translationFiles, String mavenId) {
		for (TranslationFile translationFile : translationFiles) {
			if (translationFile.getMavenId().equals(mavenId)) {
				return true;
			}
		}
		return false;
	}

	private boolean deleteFolder(File folder, boolean deleteRoot) {
		File[] listFiles = folder.listFiles();
		if (listFiles != null) {
			for (File file : listFiles) {
				if (!file.getName().startsWith(".") || deleteRoot) {
					if (file.isDirectory()) {
						deleteFolder(file, true);
					}
					if (!file.delete()) {
						return false;
					}
					getLog().debug("Deleted " + file);
				}
			}
		}
		if (deleteRoot) {
			boolean deleted = folder.delete();
			getLog().debug("Deleted " + folder);
			return deleted;
		} else {
			return true;
		}
	}

	private Map<TranslationFile, byte[]> downloadTranslations() throws MojoExecutionException {
		try {
			String uri = "http://api.crowdin.net/api/project/" + authenticationInfo.getUserName()
					+ "/download/all.zip?key=";
			getLog().debug("Calling " + uri + "<API key>");
			uri += authenticationInfo.getPassword();
			HttpGet getMethod = new HttpGet(uri);
			HttpResponse response = client.execute(getMethod);
			int returnCode = response.getStatusLine().getStatusCode();
			getLog().debug("Return code : " + returnCode);

			if (returnCode == 200) {

				Map<TranslationFile, byte[]> translations = new HashMap<TranslationFile, byte[]>();

				InputStream responseBodyAsStream = response.getEntity().getContent();
				ZipInputStream zis = new ZipInputStream(responseBodyAsStream);
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (!entry.isDirectory()) {

						String name = entry.getName();
						getLog().debug("Processing " + name);
						int slash = name.indexOf('/');
						String language = name.substring(0, slash);
						name = name.substring(slash + 1);
						slash = name.indexOf('/');
						String mavenId = null;
						if (slash > 0) {
							mavenId = name.substring(0, slash);
							name = name.substring(slash + 1);
						}
						if (name.matches("messages_.*\\.properties")) {
							name = "messages_" + CodeConversion.crowdinCodeToFileTag(language) + ".properties";
						}
						TranslationFile translationFile = new TranslationFile(CodeConversion.crowdinCodeToLanguageTag(language), mavenId, name);

						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						while (zis.available() > 0) {
							int read = zis.read();
							if (read != -1) {
								bos.write(read);
							}
						}
						bos.close();
						translations.put(translationFile, bos.toByteArray());
					}
				}

				EntityUtils.consumeQuietly(response.getEntity());
				return translations;
			} else {
				throw new MojoExecutionException("Failed to get translations from crowdin");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to call API", e);
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		getLog().info("Downloading translations from crowdin.");
		Map<TranslationFile, byte[]> translations = downloadTranslations();

		if (localRepository != null) {
			Set<Artifact> dependencyArtifacts = getAllDependencies();
			Set<String> mavenIds = new HashSet<String>();
			for (Artifact artifact : dependencyArtifacts) {
				String mavenId = getMavenId(artifact);
				mavenIds.add(mavenId);
			}

			Map<TranslationFile, byte[]> usedTranslations = new HashMap<TranslationFile, byte[]>();
			usedTranslations.putAll(translations);

			for (TranslationFile translationFile : translations.keySet()) {
				if (translationFile.getMavenId() == null) {
					getLog().debug(translationFile.getName() + " is a root project file");
				} else if (!mavenIds.contains(translationFile.getMavenId())) {
					getLog().debug(translationFile.getMavenId() + " is not a dependency");
					usedTranslations.remove(translationFile);
				} else {
					getLog().debug(translationFile.getMavenId() + " is a dependency");
				}
			}
			translations = usedTranslations;
		}

		if (translations.size() == 0) {
			getLog().info("No translations available for this project!");
		} else {

			getLog().info("Cleaning crowdin folder.");
			cleanFolders(translations.keySet());

			getLog().info("Copying translations to crowdin folder.");
			try {
				copyTranslations(translations);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to write file", e);
			}

			downloadStatus();
		}

	}

	private void downloadStatus() throws MojoExecutionException {
		getLog().info("Downloading translation status");
		Document document = crowdinRequestAPI("status", null, null, true);
		if (!document.getRootElement().getName().equals("status")) {
			String code = document.getRootElement().getChildTextNormalize("code");
			String message = document.getRootElement().getChildTextNormalize("message");
			throw new MojoExecutionException("Failed to call API for \"status\" - " + code + " - " + message);
		}

		getLog().info("Writing translation status to file");
		SortedProperties statusProperties = new SortedProperties();
		for (Object child : document.getRootElement().getChildren("language")) {
			Element childElement = (Element) child;
			if (!childElement.getChildTextTrim("code").isEmpty()) {
				String languageTag = CodeConversion.crowdinCodeToLanguageTag(childElement.getChildTextNormalize("code"));
				statusProperties.put(languageTag + ".name", childElement.getChildTextNormalize("name"));
				statusProperties.put(languageTag + ".phrases", childElement.getChildTextNormalize("phrases"));
				statusProperties.put(languageTag + ".phrases.translated", childElement.getChildTextNormalize("translated"));
				statusProperties.put(languageTag + ".phrases.approved", childElement.getChildTextNormalize("approved"));
				statusProperties.put(languageTag + ".words", childElement.getChildTextNormalize("words"));
				statusProperties.put(languageTag + ".words.translated", childElement.getChildTextNormalize("words_translated"));
				statusProperties.put(languageTag + ".words.approved", childElement.getChildTextNormalize("words_approved"));
				statusProperties.put(languageTag + ".progress.translated", childElement.getChildTextNormalize("translated_progress"));
				statusProperties.put(languageTag + ".progress.approved", childElement.getChildTextNormalize("approved_progress"));
				if (getLog().isDebugEnabled()) {
					getLog().debug(
						"Translation status for " + childElement.getChildTextNormalize("name") + "(" +
						childElement.getChildTextNormalize("code") + "): " +
						"Phrases " + childElement.getChildTextNormalize("phrases") +
						", Translated " + childElement.getChildTextNormalize("translated") +
						", Approved " + childElement.getChildTextNormalize("approved")
					);
				}
			}
		}
		File statusFile = new File(downloadFolder, statusFileName);
		try {
			FileOutputStream out = new FileOutputStream(statusFile);
			try {
				statusProperties.store(out, "This file is automatically generated, please do not edit this file.");
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to write file " + statusFile.getAbsolutePath() + ": " + e.getMessage());
		}

	}
	private void copyTranslations(Map<TranslationFile, byte[]> translations) throws IOException, MojoExecutionException {
		Set<Entry<TranslationFile, byte[]>> entrySet = translations.entrySet();
		for (Entry<TranslationFile, byte[]> entry : entrySet) {
			TranslationFile translationFile = entry.getKey();

			byte[] bytes = entry.getValue();
			SortedProperties properties = new SortedProperties();
			InputStream inStream = new ByteArrayInputStream(bytes);
			try {
				properties.load(inStream);
			} finally {
				inStream.close();
			}

			File languageFolder = new File(downloadFolder, translationFile.getLanguage());
			if (!languageFolder.exists()) {
				if (!languageFolder.mkdirs()) {
					throw new MojoExecutionException("Could not create folder " + languageFolder.getAbsolutePath());
				}
			}

			File targetFile;
			if (translationFile.getMavenId() != null) {
				File mavenIdFolder = new File(languageFolder, translationFile.getMavenId());
				if (!mavenIdFolder.exists()) {
					mavenIdFolder.mkdirs();
				}
				targetFile = new File(mavenIdFolder, translationFile.getName());
			} else {
				targetFile = new File(languageFolder, translationFile.getName());
			}

			getLog().info(
					"Importing from crowdin " + translationFile.getLanguage() + "/" + translationFile.getMavenId()
							+ "/" + translationFile.getName());

			FileOutputStream out = new FileOutputStream(targetFile);
			try {
				properties.store(out, ApplyCrowdinMojo.COMMENT);
			} finally {
				out.close();
			}
		}
	}

	private Set<Artifact> getAllDependencies() throws MojoExecutionException {
		Set<Artifact> result = new HashSet<Artifact>();
		try {
			ArtifactFilter artifactFilter = new ScopeArtifactFilter(null);

			if (localRepository != null) {
				DependencyNode rootNode = treeBuilder.buildDependencyTree(project, localRepository, artifactFactory,
						artifactMetadataSource, artifactFilter, artifactCollector);

				CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();

				rootNode.accept(visitor);

				@SuppressWarnings("unchecked")
				List<DependencyNode> nodes = visitor.getNodes();
				for (DependencyNode dependencyNode : nodes) {
					int state = dependencyNode.getState();
					Artifact artifact = dependencyNode.getArtifact();
					if (state == DependencyNode.INCLUDED) {
						result.add(new SpecialArtifact(artifact));
					}
				}
			}
		} catch (DependencyTreeBuilderException e) {
			throw new MojoExecutionException("Failed to get dependencies", e);
		}
		return result;
	}
}
