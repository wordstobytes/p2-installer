/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Mentor Graphics - Modified from original P2 stand-alone installer
 *******************************************************************************/
package com.codesourcery.internal.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.UpdateSite;

@SuppressWarnings("restriction") // Accesses internal P2 API's
public class InstallDescription implements IInstallDescription {
	/** P2 profile properties prefix */
	public static final String PROP_P2_PROFILE_PREFIX = "p2."; //$NON-NLS-1$
	/** Artifact repositories property */
	public static final String PROP_ARTIFACT_REPOSITORY = "eclipse.p2.artifacts";//$NON-NLS-1$
	/** Add-on repositories property */
	public static final String PROP_ADDON_REPOSITORY = "eclipse.p2.addons";//$NON-NLS-1$
	/** Add-on description property */
	public static final String PROP_ADDON_DESCRIPTION = "eclipse.p2.addons.description";//$NON-NLS-1$
	/** Install location property */
	public static final String PROP_INSTALL_LOCATION = "eclipse.p2.installLocation";//$NON-NLS-1$
	/** Meta-data repositories property */
	public static final String PROP_METADATA_REPOSITORY = "eclipse.p2.metadata";//$NON-NLS-1$
	/** Profile name property */
	public static final String PROP_PROFILE_NAME = "eclipse.p2.profileName";//$NON-NLS-1$
	/** <code>true</code> to remove profile on uninstall property */
	public static final String PROP_REMOVE_PROFILE = "eclipse.p2.removeProfile";//$NON-NLS-1$
	/** Required roots property */
	public static final String PROP_REQUIRED_ROOTS = "eclipse.p2.requiredRoots";//$NON-NLS-1$
	/** Optional roots property */
	public static final String PROP_OPTIONAL_ROOTS = "eclipse.p2.optionalRoots";//$NON-NLS-1$
	/** License property */
	public static final String PROP_LICENSE = "eclipse.p2.license";//$NON-NLS-1$
	/** License installable units property */
	public static final String PROP_LICENSE_IU = "eclipse.p2.licenseIU";//$NON-NLS-1$
	/** Install information property */
	public static final String PROP_INFORMATION = "eclipse.p2.information";//$NON-NLS-1$
	/** Optional roots default property */
	public static final String PROP_OPTIONAL_ROOTS_DEFAULT = "eclipse.p2.optionalRootsDefault";//$NON-NLS-1$
	/** Product identifier property */
	public static final String PROP_PRODUCT_ID = "eclipse.p2.productId";//$NON-NLS-1$
	/** Product name property */
	public static final String PROP_PRODUCT_NAME = "eclipse.p2.productName";//$NON-NLS-1$
	/** Vendor name property */
	public static final String PROP_PRODUCT_VENDOR = "eclipse.p2.productVendor";//$NON-NLS-1$
	/** Product version property */
	public static final String PROP_PRODUCT_VERSION = "eclipse.p2.productVersion";//$NON-NLS-1$
	/** Product help link property */
	public static final String PROP_PRODUCT_HELP = "eclipse.p2.productHelp";//$NON-NLS-1$
	/** Root install location property */
	public static final String PROP_ROOT_LOCATION = "eclipse.p2.rootLocation";//$NON-NLS-1$
	/** Uninstall files property */
	public static final String PROP_UNINSTALL_FILES = "eclipse.p2.uninstallFiles";//$NON-NLS-1$
	/** Links property */
	public static final String PROP_LINKS = "eclipse.p2.links";//$NON-NLS-1$
	/** Links location property */
	public static final String PROP_LINKS_LOCATION = "eclipse.p2.linksLocation";//$NON-NLS-1$
	/** Links default property */
	public static final String PROP_LINKS_DEFAULT = "eclipse.p2.linksDefault";//$NON-NLS-1$
	/** Environment paths property */
	public static final String PROP_ENV_PATHS = "eclipse.p2.env.paths";//$NON-NLS-1$
	/** Launch items property */
	public static final String PROP_LAUNCH = "eclipse.p2.launch";//$NON-NLS-1$
	/** Installer window title property */
	public static final String PROP_WINDOW_TITLE = "eclipse.p2.windowTitle";//$NON-NLS-1$
	/** Installer title property */
	public static final String PROP_TITLE = "eclipse.p2.title";//$NON-NLS-1$
	/** Installer wizard pages property */
	public static final String PROP_WIZARD_PAGES = "eclipse.p2.wizardPages";//$NON-NLS-1$
	/** Progress find regular expression property */
	public static final String PROP_PROGRESS_FIND = "eclipse.p2.progressFind";//$NON-NLS-1$
	/** Progress replace regular expression property */
	public static final String PROP_PROGRESS_REPLACE = "eclipse.p2.progressReplace";//$NON-NLS-1$
	/** Installer modules property */
	public static final String PROP_MODULES = "eclipse.p2.modules";//$NON-NLS-1$
	/** Sort roots property */
	public static final String PROP_SORT_ROOTS = "eclipse.p2.sortRoots";//$NON-NLS-1$
	/** Sort optional roots property */
	public static final String PROP_SORT_OPTIONAL_ROOTS = "eclipse.p2.sortOptionalRoots";//$NON-NLS-1$
	/** Update sites property */
	private static final String PROP_UPDATE = "eclipse.p2.update";//$NON-NLS-1$
	/** Hide Components version property */
	public static final String PROP_HIDE_COMPONENTS_VERSION = "eclipse.p2.hideComponentsVersion";//$NON-NLS-1$
	/** Add-ons require login property */
	public static final String PROP_ADDONS_REQUIRE_LOGIN = "eclipse.p2.addons.requiresLogin";//$NON-NLS-1$
	/** Install wizard page navigation property */
	public static final String PROP_WIZARD_NAVIGATION = "org.eclipse.p2.wizardNavigation";//$NON-NLS-1$
		
	/** Base location for installer */
	private URI base;
	/** Installer properties */
	private Map<String, String> properties = new HashMap<String, String>();
	/** P2 profile properties */
	private Map<String, String> profileProperties = new HashMap<String, String>();
	/** P2 artifact repository locations */
	private URI[] artifactRepos;
	/** P2 meta-data repository locations */
	private URI[] metadataRepos;
	/** Add-ons repository locations */
	private URI[] addonRepos;
	/** Add-ons description */
	private String addonDescription;
	/** Root install location */
	private IPath rootLocation;
	/** P2 install location */
	private IPath p2Location;
	/** Items to launch after installation */
	private LaunchItem[] launchItems;
	/** Update sites to add */
	private UpdateSite[] updateSites;
	/** Product identifier */
	private String productId;
	/** Product name */
	private String productName;
	/** Product vendor */
	private String productVendor;
	/** Product version */
	private String productVersion;
	/** Product help URL */
	private String productHelp;
	/** Required root installable units */
	private IVersionedId[] requiredRoots;
	/** Optional root installable units */
	private IVersionedId[] optionalRoots;
	/** Default optional root installable units */
	private IVersionedId[] optionalRootsDefault;
	/** Product licenses */
	private LicenseDescriptor[] licenses;
	/** Include license IU */
	private boolean licenseIU;
	/** Install information text */
	private String informationText;
	/** P2 profile identifier */
	private String profileName;
	/** <code>true</code> to remove entire profile on uninstall */
	private boolean removeProfile = false;
	/** Files to copy for uninstaller */
	private String[] uninstallFiles;
	/** Uninstaller name */
	private String uninstallerName;
	/** Short-cuts to create */
	private LinkDescription[] links;
	/** Short-cuts location */
	private IPath linksLocation;
	/** Environment pats to add */
	private String[] environmentPaths;
	/** Installer window title */
	private String windowTitle;
	/** Installer title */
	private String title;
	/** Install wizard pages */
	private String[] wizardPages;
	/** Progress regular expression find patterns */
	private String[] progressFindPatterns;
	/** Progress regular expression replace patterns */
	private String[] progressReplacePatterns;
	/** Active module identifiers */
	private String[] moduleIDs;
	/** <code>true</code> to sort required roots */
	private boolean sortRoots;
	/** <code>true</code> to sort optional roots */
	private boolean sortOptionalRoots;
	/** <code>true</code> to hide components version */
	private boolean hideComponentsVersion = false;
	/** <code>true</code> if add-ons require login */
	private boolean addonsRequiresLogin;
	/** Install wizard page navigation */
	private PageNavigation pageNavigation;

	/**
	 * Loads an install description.
	 * 
	 * @param site Site for description
	 * @param props Install property values to set or <code>null</code>
	 * description file or <code>null</code> to use default
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void load(URI site, Map<String, String> props, IProgressMonitor monitor) 
			throws CoreException {
		try {
			InputStream in = null;
			try {
				in = new RepositoryTransport().stream(site, monitor);
				properties = CollectionUtils.loadProperties(in);
			} finally {
				if (in != null) {
					try {
						in.close();
					}
					catch (IOException e) {
						// Ignore
					}
				}
			}

			// Replace properties
			if (props != null) {
				Iterator<Entry<String, String>> iter = props.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<String, String> prop = iter.next();
					String name = prop.getKey();
					String value = prop.getValue();
					properties.put(name, value);
				}
			}
			
			// Resolve variables
			resolveVariables(properties);
			
			base = getBase(site);
			// Load properties
			initialize(properties);
			// Load profile properties
			initializeProfileProperties(properties);
			// Override the properties from anything interesting in system properties
			initialize(CollectionUtils.toMap(System.getProperties()));
		}
		catch (Exception e) {
			Installer.fail("Failed to load install description.", e);
		}
	}
	
	/**
	 * Resolves property variables, replacing any ${property} with the value
	 * of the 'property'.
	 * 
	 * @param properties Properties to replace
	 */
	private void resolveVariables(Map<String, String> properties) {
		try {
			Iterator<Entry<String, String>> iter = properties.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> entry = iter.next();
				String value = entry.getValue();
				int index, index2;
				// Start of variable
				while ((index = value.indexOf('$')) != -1) {
					if ((index + 1 < value.length()) && (value.charAt(index + 1) == '{')) {
						index2 = value.indexOf('}', index);
						if (index2 != -1) {
							// Property to replace
							String property = value.substring(index + 2, index2);
							// Property value
							String sub = properties.get(property);
							// Replace variable
							value = value.substring(0, index) + sub + value.substring(index2 + 1);
							entry.setValue(value);
						}
						else {
							Installer.log("Error in resolving macros: No matching \"}\" for \"${\".");
							break;
						}
					}
					else {
						break;
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}

	/**
	 * Returns the base URI.
	 * 
	 * @return Base URI
	 */
	protected URI getBase() {
		return base;
	}
	
	/**
	 * Initializes the install description.
	 * 
	 * @param properties Properties
	 */
	protected void initialize(Map<String, String> properties) {
		// P2 artifact repositories
		String property = properties.get(PROP_ARTIFACT_REPOSITORY);
		if (property != null)
			setArtifactRepositories(getURIs(property, getBase()));

		// P2 meta-data repositories
		property = properties.get(PROP_METADATA_REPOSITORY);
		if (property != null)
			setMetadataRepositories(getURIs(property, getBase()));

		// P2 add-on repositories
		property = properties.get(PROP_ADDON_REPOSITORY);
		if (property != null)
			setAddonRepositories(getURIs(property, getBase()));

		// P2 add-ons description
		property = properties.get(PROP_ADDON_DESCRIPTION);
		if (property != null)
			setAddonDescription(property);

		// Update sites
		property = properties.get(PROP_UPDATE);
		if (property != null) {
			ArrayList<UpdateSite> updateSites = new ArrayList<UpdateSite>();
			String[] sites = getArrayFromString(property, ",");
			for (String site : sites) {
				String[] parts = getArrayFromString(site, ";");
				UpdateSite updateSite = new UpdateSite(parts[0], parts.length == 2 ? parts[1] : null);
				updateSites.add(updateSite);
			}
			setUpdateSites(updateSites.toArray(new UpdateSite[updateSites.size()]));
		}
		
		// Items to launch
		property = properties.get(PROP_LAUNCH);
		if (property != null) {
			ArrayList<LaunchItem> launchItems = new ArrayList<LaunchItem>();
			
			String[] programs = getArrayFromString(property, ",");
			for (String program : programs) {
				String[] parts = getArrayFromString(program, ";");

				// Launch type
				LaunchItemType type;
				// Executable
				if (parts[2].equals("exe"))
					type = LaunchItemType.EXECUTABLE;
				// File
				else if (parts[2].equals("file"))
					type = LaunchItemType.FILE;
				// URL
				else 
					type = LaunchItemType.HTML;
				
				parts[1] = parts[1].replace("{exe}", Installer.isWindows() ? ".exe" : "");
				
				// Default selection
				boolean selected = true;
				if (parts.length > 3)
					selected = Boolean.parseBoolean(parts[3]);
				
				LaunchItem item = new LaunchItem(type, parts[0], parts[1], selected);
				launchItems.add(item);
			}
			setLaunchItems(launchItems.toArray(new LaunchItem[launchItems.size()]));
		}

		// Default install location
		property = properties.get(PROP_INSTALL_LOCATION);
		if (property != null) {
			setInstallLocation(getPath(property));
		}

		// Profile name
		property = properties.get(PROP_PROFILE_NAME);
		if (property != null)
			setProfileName(property);
		
		// Remove profile
		property = properties.get(PROP_REMOVE_PROFILE);
		if (property != null)
			setRemoveProfile(property.trim().toLowerCase().equals("true"));

		// Sort roots
		property = properties.get(PROP_SORT_ROOTS);
		if (property != null)
			setSortRequiredComponents(property.trim().toLowerCase().equals("true"));

		// Sort optional roots
		property = properties.get(PROP_SORT_OPTIONAL_ROOTS);
		if (property != null)
			setSortOptionalComponents(property.trim().toLowerCase().equals("true"));

		// Product identifier
		property = properties.get(PROP_PRODUCT_ID);
		if (property != null)
			setProductId(property);

		// Product name
		property = properties.get(PROP_PRODUCT_NAME);
		if (property != null)
			setProductName(property);
		
		// Product vendor
		property = properties.get(PROP_PRODUCT_VENDOR);
		if (property != null)
			setProductVendor(property);
		
		// Product version
		property = properties.get(PROP_PRODUCT_VERSION);
		if (property != null)
			setProductVersion(property);
		
		// Product help
		property = properties.get(PROP_PRODUCT_HELP);
		if (property != null)
			setProductHelp(property);

		// Required roots
		String rootSpec = properties.get(PROP_REQUIRED_ROOTS);
		if (rootSpec != null) {
			String[] rootList = getArrayFromString(rootSpec, ","); //$NON-NLS-1$
			ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>(rootList.length);
			for (int i = 0; i < rootList.length; i++) {
				try {
					roots.add(VersionedId.parse(rootList[i]));
				} catch (IllegalArgumentException e) {
					LogHelper.log(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_InvalidInstallDescriptionVersion + rootList[i], e)); //$NON-NLS-1$
				}
			}
			if (!roots.isEmpty()) {
				setRequiredRoots(roots.toArray(new IVersionedId[roots.size()]));
			}
		}
		
		// Optional roots
		rootSpec = properties.get(PROP_OPTIONAL_ROOTS);
		if (rootSpec != null) {
			String[] rootList = getArrayFromString(rootSpec, ",");
			ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>(rootList.length);
			// Loop through optional roots
			for (String root : rootList) {
				// Add optional root
				try {
					roots.add(VersionedId.parse(root));
				} catch (IllegalArgumentException e) {
					LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Invalid version in install description: " + root, e)); //$NON-NLS-1$
				}
			}
			// Set optional roots
			if (!roots.isEmpty()) {
				setOptionalRoots(roots.toArray(new IVersionedId[roots.size()]));
			}
		}

		// Default optional roots
		rootSpec = properties.get(PROP_OPTIONAL_ROOTS_DEFAULT);
		if (rootSpec != null) {
			String[] rootList = getArrayFromString(rootSpec, ","); //$NON-NLS-1$
			ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>(rootList.length);
			for (int i = 0; i < rootList.length; i++) {
				try {
					roots.add(VersionedId.parse(rootList[i]));
				} catch (IllegalArgumentException e) {
					LogHelper.log(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_InvalidInstallDescriptionVersion + rootList[i], e)); //$NON-NLS-1$
				}
			}
			if (!roots.isEmpty())
				setDefaultOptionalRoots(roots.toArray(new IVersionedId[roots.size()]));
		}
		
		// Licenses
		property = properties.get(PROP_LICENSE);
		if (property != null) {
			ArrayList<LicenseDescriptor> licenses = new ArrayList<LicenseDescriptor>();
			
			String[] licenseInfos = getArrayFromString(property, ",");  //$NON-NLS-1$
			int licenseCounter = 0;
			for (String licenseInfo : licenseInfos) {
				String licenseName = null;

				int index = licenseInfo.indexOf(':');
				if (index != -1) {
					licenseName = licenseInfo.substring(index + 1);
					licenseInfo = licenseInfo.substring(0, index);
				}
				
				URI[] licenseLocations = getURIs(licenseInfo, getBase());
				if (licenseLocations.length > 0) {
					try {
						String contents = readFile(licenseLocations[0]);
						if (contents != null) {
							// License Name is not provided
							if (licenseName == null) {
								licenseName = NLS.bind(InstallMessages.LicensePageTitle0, Integer.toString(++licenseCounter));
							}
							LicenseDescriptor license = new LicenseDescriptor(readFile(licenseLocations[0]), licenseName);
							licenses.add(license);
						}
						else {
							Installer.log("Failed to read license file: " + licenseLocations[0]);
						}
					} catch (IOException e) {
						LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Failed to read license file: " + licenseLocations[0], e)); //$NON-NLS-1$
					}
				}
			}
			
			setLicenses(licenses.toArray(new LicenseDescriptor[licenses.size()]));
		}
		
		// License IUs
		property = properties.get(PROP_LICENSE_IU);
		if (property != null) {
			setLicenseIU(property.trim().toLowerCase().equals("true"));
		}
		
		// Information
		property = properties.get(PROP_INFORMATION);
		if (property != null) {
			URI[] informationLocations = getURIs(property, getBase());
			if (informationLocations.length > 0) {
				try {
					setInformationText(readFile(informationLocations[0]));
				} catch (IOException e) {
					LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Failed to read information file: " + informationLocations[0], e)); //$NON-NLS-1$
				}
			}
		}
		
		// Uninstall files
		property = properties.get(PROP_UNINSTALL_FILES);
		if (property != null) {
			String[] uninstallFiles = getArrayFromString(property, ","); //$NON-NLS-1$
			String uninstallFile;
			String uninstallerFileName = IInstallConstants.INSTALLER_NAME;
			// Replace executable extensions
			for (int index = 0; index < uninstallFiles.length; index++) {
				uninstallFile = uninstallFiles[index];
				if (uninstallFile.contains(":") && uninstallFile.startsWith("setup{exe}")) {
					uninstallerFileName = uninstallFile.substring(uninstallFile.indexOf(":") + 1);
					uninstallerFileName = uninstallerFileName.substring(0,uninstallerFileName.indexOf("{exe}"));
				}
				uninstallFiles[index] = uninstallFiles[index].replace("{exe}", 
					Installer.isWindows() ? "." + IInstallConstants.EXTENSION_EXE : "");
			}
			setUninstallFiles(uninstallFiles);
			setUninstallerName(uninstallerFileName);
		}
		// Root location
		property = properties.get(PROP_ROOT_LOCATION);
		if (property != null) {
			setRootLocation(getPath(property));
		}
		
		// Short-cuts links location
		property = properties.get(PROP_LINKS_LOCATION);
		if (property != null) {
			String safeName = InstallUtils.makeFileNameSafe(property);
			setLinksLocation(new Path(safeName));
		}
		
		// Default short-cut link folders
		ArrayList<ShortcutFolder> defaultShortcutFolders = new ArrayList<ShortcutFolder>();
		property = properties.get(PROP_LINKS_DEFAULT);
		if (property != null) {
			String[] folders = getArrayFromString(property, ",");
			for (String folder : folders) {
				if (folder.equals("programs"))
					defaultShortcutFolders.add(ShortcutFolder.PROGRAMS);
				else if (folder.equals("desktop"))
					defaultShortcutFolders.add(ShortcutFolder.DESKTOP);
				else if (folder.equals("launcher"))
					defaultShortcutFolders.add(ShortcutFolder.UNITY_DASH);
			}
		}

		// Short-cut links
		property = properties.get(PROP_LINKS);
		if (property != null) {
			ArrayList<LinkDescription> linkDescriptions = new ArrayList<LinkDescription>();
			
			String[] links = getArrayFromString(property, ",");
			for (String link : links) {
				String[] parts = getArrayFromString(link, ";");
				// Replace executable extension (if specified)
				parts[3] = parts[3].replace("{exe}", Installer.isWindows() ? ".exe" : "");
				String folderSpec = parts[0].trim();
				ShortcutFolder folder = null;
				if (folderSpec.equals("programs"))
					folder = ShortcutFolder.PROGRAMS;
				else if (folderSpec.equals("desktop"))
					folder = ShortcutFolder.DESKTOP;
				else if (folderSpec.equals("launcher"))
					folder = ShortcutFolder.UNITY_DASH;

				// Default to selected if optional defaults not specified
				boolean selected = defaultShortcutFolders.size() == 0;
				// Else, check if folder is selected by default
				for (ShortcutFolder defaultFolder : defaultShortcutFolders) {
					if (folder == defaultFolder) {
						selected = true;
						break;
					}
				}
				
				String[] args = getArrayFromString(parts[5], ",");
				
				// Add link description
				LinkDescription linkDescription = new LinkDescription(
						folder,				// Folder
						parts[1].trim(),	// Link path
						parts[2].trim(),	// Link name
						parts[3].trim(),	// Link target
						parts[4].trim(),	// Icon path
						args,				// Launcher arguments
						selected			// Default selection
						);
				linkDescriptions.add(linkDescription);
			}
			setLinks(linkDescriptions.toArray(new LinkDescription[linkDescriptions.size()]));
		}
		
		// Environment path variables
		property = properties.get(PROP_ENV_PATHS);
		if (property != null) {
			String[] paths = getArrayFromString(property, ",");
			setEnvironmnetPaths(paths);
		}
		
		// Title
		property = properties.get(PROP_TITLE);
		if (property != null) {
			setTitle(property);
		}
		// Window title
		property = properties.get(PROP_WINDOW_TITLE);
		if (property != null) {
			setWindowTitle(property);
		}
		
		// Wizard pages
		property = properties.get(PROP_WIZARD_PAGES);
		if (property != null) {
			String[] pages = getArrayFromString(property, ",");
			setWizardPagesOrder(pages);
		}
		
		// P2 progress find/replace
		if (properties.get(PROP_PROGRESS_FIND + "0") != null) {
			int index = 0;
			ArrayList<String> findPatterns = new ArrayList<String>();
			ArrayList<String> replacePatterns = new ArrayList<String>();
			for (;;) {
				String indexPostfix = Integer.toString(index);
				// Find regular expression
				String find = properties.get(PROP_PROGRESS_FIND + indexPostfix);
				if (find == null)
					break;
				// Corresponding replace expression
				String replace = properties.get(PROP_PROGRESS_REPLACE + indexPostfix);
				if (replace == null)
					break;
				findPatterns.add(find);
				replacePatterns.add(replace);
				index ++;
			}
			setProgressFindPatterns(findPatterns.toArray(new String[findPatterns.size()]));
			setProgressReplacePatterns(replacePatterns.toArray(new String[findPatterns.size()]));
		}
		
		property = properties.get(PROP_MODULES);
		if (property != null) {
			String[] moduleList = getArrayFromString(property, ","); //$NON-NLS-1$
			setModuleIDs(moduleList);
		}
		
		// Show/Hide components version on components page
		property = properties.get(PROP_HIDE_COMPONENTS_VERSION);
		if (property != null) {
			setHideComponentsVersion(property.trim().toLowerCase().equals("true"));
		}

		// Add-ons require login
		property = properties.get(PROP_ADDONS_REQUIRE_LOGIN);
		if (property != null) {
			setAddonsRequireLogin(property.trim().toLowerCase().equals("true"));
		}
		
		// Wizard page navigation
		property = properties.get(PROP_WIZARD_NAVIGATION);
		if (property != null) {
			PageNavigation navigation = PageNavigation.NONE;
			property = property.trim().toLowerCase();
			if (property.equals("top"))
				navigation = PageNavigation.TOP;
			else if (property.equals("left"))
				navigation = PageNavigation.LEFT;
			setPageNavigation(navigation);
		}
	}

	/**
	 * Add all of the given properties to profile properties of the given description 
	 * after removing the keys known to be for the installer.  This allows install descriptions 
	 * to also set random profile properties.
	 * @param properties
	 */
	private void initializeProfileProperties(Map<String, String> properties) {
		// Load profile properties
		Map<String, String> profileProperties = new HashMap<String, String>();
		Set<Entry<String, String>> entries = properties.entrySet();
		int prefixLength = PROP_P2_PROFILE_PREFIX.length();
		for (Entry<String, String> entry : entries) {
			String key = entry.getKey();
			if (key.startsWith(PROP_P2_PROFILE_PREFIX)) {
				String property = key.substring(prefixLength);
				profileProperties.put(property, entry.getValue());
			}
		}
		setProfileProperties(profileProperties);
	}
	
	/**
	 * Returns the base URI for a given
	 * URI.
	 * 
	 * @param uri URI
	 * @return Base URI or <code>null</code>.
	 */
	protected static URI getBase(URI uri) {
		if (uri == null)
			return null;

		String uriString = uri.toString();
		int slashIndex = uriString.lastIndexOf('/');
		if (slashIndex == -1 || slashIndex == (uriString.length() - 1))
			return uri;

		return URI.create(uriString.substring(0, slashIndex + 1));
	}

	/**
	 * Normalizes a path to have platform
	 * directory separators.
	 * 
	 * @param path Path
	 * @return Normalized path
	 */
	protected static IPath getPath(String path) {
		path = path.trim().replace('\\', File.separatorChar);
		path = path.replace('/', File.separatorChar);
		// Replace home directory
		path = path.replace("~", System.getProperty("user.home"));
		
		return new Path(path);
	}

	/**
	 * Reads the contents of a file.
	 * 
	 * @param location File location
	 * @return File contents
	 * @throws IOException on failure to read the file.
	 */
	protected String readFile(URI location) throws IOException {
		String contents = null;
		File file = new File(location);
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				StringBuilder buffer = new StringBuilder();
				String line;
				String separator = System.getProperty("line.separator"); //$NON-NLS-1$
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
					buffer.append(separator);
				}
				contents = buffer.toString();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}

		return contents;
	}

	/**
	 * Returns a URI for a specification.
	 * 
	 * @param spec Specification
	 * @param base Base location
	 * @return URI or <code>null</code>
	 */
	protected URI getURI(String spec, URI base) {
		URI uri = null;
		try {
			uri = URIUtil.fromString(spec);
			String uriScheme = uri.getScheme();
			// Handle jar scheme special to support relative paths
			if ((uriScheme != null) && uriScheme.equals("jar")) { //$NON-NLS-1$
				String path = uri.getSchemeSpecificPart().substring("file:".length()); //$NON-NLS-1$
				URI relPath = URIUtil.append(base, path);
				uri = new URI("jar:" + relPath.toString());  //$NON-NLS-1$
			}
			else {
				uri = URIUtil.makeAbsolute(uri, base);
			}
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Invalid URL in install description: " + spec, e)); //$NON-NLS-1$
		}
		
		return uri;
	}
	
	/**
	 * Returns an array of URIs from the given comma-separated list
	 * of URLs. Returns <code>null</code> if the given spec does not contain any URLs.
	 * @param base Base location 
	 * @return An array of URIs in the given spec, or <code>null</code>
	 */
	protected URI[] getURIs(String spec, URI base) {
		if (spec.trim().isEmpty())
			return new URI[0];
		
		String[] urlSpecs = getArrayFromString(spec, ","); //$NON-NLS-1$
		ArrayList<URI> result = new ArrayList<URI>(urlSpecs.length);
		for (int i = 0; i < urlSpecs.length; i++) {
			URI uri = getURI(urlSpecs[i], base);
			if (uri != null) {
				result.add(uri);
			}
		}
		if (result.isEmpty())
			return null;
		return result.toArray(new URI[result.size()]);
	}
	
	/**
	 * Returns an array of strings from a single string of tokens delimited
	 * by a separator.
	 * 
	 * @param list List of tokens
	 * @param separator Separator
	 * @return Array of separated strings
	 */
	protected String[] getArrayFromString(String list, String separator) {
		ArrayList<String> parts = new ArrayList<String>();
		
		int start = 0;
		int end = list.indexOf(separator);
		if (end == -1) {
			if (list.trim().isEmpty()) {
				return new String[] {};
			}
			return new String[] { list };
		}
		else {
			String part;
			while (end != -1) {
				part = list.substring(start, end).trim();
				parts.add(part);
				start = end + 1;
				end = list.indexOf(separator, start);
			}
			if (start < list.length()) {
				part = list.substring(start);
				parts.add(part);
			}
			
		}
		
		return parts.toArray(new String[parts.size()]);
	}

	@Override
	public String getProperty(String propertyName) {
		return properties.get(propertyName);
	}

	@Override
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
	
	/**
	 * Returns all installer properties.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}
	
	@Override
	public void setWindowTitle(String value) {
		windowTitle = value;
	}
	
	@Override
	public String getWindowTitle() {
		return windowTitle;
	}

	@Override
	public void setTitle(String value) {
		title = value;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setProfileProperties(Map<String, String> properties) {
		profileProperties.putAll(properties);
	}
	
	@Override
	public Map<String, String> getProfileProperties() {
		return profileProperties;
	}

	@Override
	public void setEnvironmnetPaths(String[] paths) {
		this.environmentPaths = paths;
	}
	
	@Override
	public String[] getEnvironmentPaths() {
		return environmentPaths;
	}
	
	@Override
	public void setArtifactRepositories(URI[] value) {
		this.artifactRepos = value;
	}

	@Override
	public URI[] getArtifactRepositories() {
		return artifactRepos;
	}

	@Override
	public void setInstallLocation(IPath location) {
		this.p2Location = location;
	}
	
	@Override
	public IPath getInstallLocation() {
		return getRootLocation().append(p2Location);
	}

	@Override
	public void setLaunchItems(LaunchItem[] value) {
		this.launchItems = value;
	}

	@Override
	public LaunchItem[] getLaunchItems() {
		return launchItems;
	}

	@Override
	public void setMetadataRepositories(URI[] value) {
		this.metadataRepos = value;
	}

	@Override
	public URI[] getMetadataRepositories() {
		return metadataRepos;
	}

	@Override
	public void setAddonRepositories(URI[] value) {
		this.addonRepos = value;
	}
	
	@Override
	public URI[] getAddonRepositories() {
		return addonRepos;
	}

	@Override
	public void setAddonDescription(String addonDescription) {
		this.addonDescription = addonDescription;
	}

	@Override
	public String getAddonDescription() {
		return addonDescription;
	}

	@Override
	public void setUpdateSites(UpdateSite[] updateSites) {
		this.updateSites = updateSites;
	}

	@Override
	public UpdateSite[] getUpdateSites() {
		return updateSites;
	}

	@Override
	public void setProductId(String value) {
		productId = value;
	}
	
	@Override
	public String getProductId() {
		return productId;
	}

	@Override
	public void setProductName(String value) {
		productName = value;
	}
	
	@Override
	public String getProductName() {
		return productName;
	}

	@Override
	public void setProductVendor(String value) {
		productVendor = value;
	}
	
	@Override
	public String getProductVendor() {
		return productVendor;
	}

	@Override
	public void setProductVersion(String value) {
		productVersion = value;
	}
	
	@Override
	public String getProductVersion() {
		return productVersion;
	}

	@Override
	public void setProductHelp(String value) {
		productHelp = value;
	}
	
	@Override
	public String getProductHelp() {
		return productHelp;
	}

	@Override
	public void setProfileName(String value) {
		profileName = value;
	}

	@Override
	public String getProfileName() {
		return profileName;
	}

	@Override
	public void setSortRequiredComponents(boolean value) {
		this.sortRoots = value;
	}

	@Override
	public void setSortOptionalComponents(boolean value) {
		this.sortOptionalRoots = value;
	}
	
	@Override
	public boolean getSortRequiredComponents() {
		return sortRoots;
	}
	
	@Override
	public boolean getSortOptionalComponents() {
		return sortOptionalRoots;
	}

	@Override
	public void setRemoveProfile(boolean removeProfile) {
		this.removeProfile = removeProfile;
	}
	
	@Override
	public boolean getRemoveProfile() {
		return removeProfile;
	}

	@Override
	public void setLicenses(LicenseDescriptor[] value) {
		licenses = value;
	}
	
	@Override
	public LicenseDescriptor[] getLicenses() {
		return licenses;
	}

	@Override
	public void setInformationText(String value) {
		informationText = value;
	}
	
	@Override
	public String getInformationText() {
		return informationText;
	}

	@Override
	public void setUninstallFiles(String[] value) {
		uninstallFiles = value;
	}
	
	@Override
	public String[] getUninstallFiles() {
		return uninstallFiles;
	}

	@Override
	public void setModuleIDs(String[] value) {
		moduleIDs = value;
	}

	@Override
	public String[] getModuleIDs() {
		return moduleIDs;
	}

	@Override
	public void setRootLocation(IPath location) {
		this.rootLocation = location;
	}

	@Override
	public IPath getRootLocation() {
		return rootLocation;
	}
	
	@Override
	public void setRequiredRoots(IVersionedId[] value) {
		requiredRoots = value;
	}

	@Override
	public IVersionedId[] getRequiredRoots() {
		return requiredRoots;
	}

	@Override
	public void setOptionalRoots(IVersionedId[] value) {
		optionalRoots = value;
	}
	
	@Override
	public IVersionedId[] getOptionalRoots() {
		return optionalRoots;
	}

	@Override
	public void setDefaultOptionalRoots(IVersionedId[] value) {
		optionalRootsDefault = value;
	}
	
	@Override
	public IVersionedId[] getDefaultOptionalRoots() {
		return optionalRootsDefault;
	}

	@Override
	public void setLinks(LinkDescription[] value) {
		links = value;
	}

	@Override
	public LinkDescription[] getLinks() {
		return links;
	}

	@Override
	public void setLinksLocation(IPath value) {
		linksLocation = value;
	}
	
	@Override
	public IPath getLinksLocation() {
		return linksLocation;
	}

	@Override
	public void setUninstallerName(String name) {
		uninstallerName = name;
	}
	
	@Override
	public String getUninstallerName() {
		return uninstallerName;
	}
	
	@Override
	public void setWizardPagesOrder(String[] wizardPages) {
		this.wizardPages = wizardPages;
	}
	
	@Override
	public String[] getWizardPagesOrder() {
		return wizardPages;
	}
	
	@Override
	public void setProgressFindPatterns(String[] value) {
		this.progressFindPatterns = value;
	}
	
	@Override
	public String[] getProgressFindPatterns() {
		return progressFindPatterns;
	}

	@Override
	public void setProgressReplacePatterns(String[] value) {
		this.progressReplacePatterns = value;
	}

	@Override
	public String[] getProgressReplacePatterns() {
		return progressReplacePatterns;
	}
	
	@Override
	public void setHideComponentsVersion(boolean hideComponentsVersion) {
		this.hideComponentsVersion = hideComponentsVersion;
	}

	@Override
	public boolean getHideComponentsVersion() {
		return this.hideComponentsVersion;
	}

	@Override
	public void setAddonsRequireLogin(boolean requiresLogin) {
		this.addonsRequiresLogin = requiresLogin;
	}

	@Override
	public boolean getAddonsRequireLogin() {
		return addonsRequiresLogin;
	}

	@Override
	public void setPageNavigation(PageNavigation pageNavigation) {
		this.pageNavigation = pageNavigation;
	}

	@Override
	public PageNavigation getPageNavigation() {
		return pageNavigation;
	}

	@Override
	public void setLicenseIU(boolean value) {
		this.licenseIU = value;
	}

	@Override
	public boolean getLicenseIU() {
		return licenseIU;
	}
}
