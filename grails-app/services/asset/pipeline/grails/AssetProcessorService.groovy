package asset.pipeline.grails


import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator

import static asset.pipeline.AssetHelper.fileForFullName


class AssetProcessorService {

	static transactional = false


	def grailsApplication


	/**
	 * Retrieves the asset path from the property [grails.assets.mapping] which is used by the url mapping and the
	 * taglib.  The property cannot contain <code>/</code>, and must be one level deep
	 *
	 * @return the path
	 * @throws IllegalArgumentException if the path contains <code>/</code>
	 */
	String getAssetMapping() {
		def path = grailsApplication.config?.grails?.assets?.mapping ?: 'assets'
		if (path.contains('/')) {
			throw new IllegalArgumentException(
				'The property [grails.assets.mapping] can only be one level deep.  ' +
				"For example, 'foo' and 'bar' would be acceptable values, but 'foo/bar' is not"
			)
		}

		return path
	}


	String getAssetPath(final String path, final ConfigObject conf = grailsApplication.config.grails.assets) {
		path && conf.precompiled \
			? conf.manifest.getProperty(path) ?: path
			: path
	}


	boolean isAssetPath(final String path, final ConfigObject conf = grailsApplication.config.grails.assets) {
		conf.precompiled \
			? conf.manifest.getProperty(path)
			: fileForFullName(path) != null
	}


	String asset(Map attrs, DefaultLinkGenerator linkGenerator) {
		def absolutePath = linkGenerator.handleAbsolute(attrs)

		def conf = grailsApplication.config.grails.assets
		def url  = attrs.file ?: attrs.src
		def assetFound = false

		if (url) {
			if (conf.precompiled) {
				def realPath = conf.manifest.getProperty(url)
				if (realPath) {
					url = assetUriRootPath() + realPath
					assetFound = true
				}
			}
			else {
				def assetFile = fileForFullName(url)
				if (assetFile != null) {
					url = assetUriRootPath() + url
					assetFound = true
				}
			}
		}

		if (!assetFound) {
			return null
		}
		else {
			if (!url?.startsWith('http')) {
				final contextPathAttribute = attrs.contextPath?.toString()
				if (absolutePath == null) {
					final cp = contextPathAttribute == null ? linkGenerator.getContextPath() : contextPathAttribute
					if (cp == null) {
						absolutePath = linkGenerator.handleAbsolute(absolute:true)
					}
					else {
						absolutePath = cp
					}
				}
				url = (absolutePath?:'') + (url ?: '')
			}
			return url
		}
	}

	private String assetUriRootPath() {
		final def url = grailsApplication.config.grails.assets.url
		url instanceof Closure \
			? url.call(null)
			: url ?: "/$assetMapping/"
	}
}
