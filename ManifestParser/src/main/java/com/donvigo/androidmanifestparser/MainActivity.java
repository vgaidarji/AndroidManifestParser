package com.donvigo.androidmanifestparser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.donvigo.androidmanifestparser.manifest.ActivityEntry;
import com.donvigo.androidmanifestparser.manifest.AndroidManifest;
import com.donvigo.androidmanifestparser.manifest.IntentCategory;
import com.donvigo.androidmanifestparser.manifest.IntentFilterEntry;
import com.donvigo.androidmanifestparser.xml.AndroidXMLDecompress;
import com.donvigo.androidmanifestparser.xml.XmlHandler;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;


public class MainActivity extends ActionBarActivity {

    private static final String APK_EXTENSION = ".apk";
    private static final String ANDROID_MANIFEST_FILE = "AndroidManifest.xml";

    private Map<String, InstalledPackageInfo> installedPackages;
    private Set<String> installedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        installedPackages = Collections.synchronizedMap(new HashMap<String, InstalledPackageInfo>());
        installedFiles = new HashSet<String>();

        // call folders scan
        scanFolder("/data/app/"); // only available if we have ReadWrite access to /data/ directory. we will request files using root shell.
        scanFolder("/system/app/");

        // NOTE: result is printed in log and saved in installedPackages map
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanFolder(String folderPath) {
        if (folderPath == null) return;

        File folder = new File(folderPath);
        if(folder.isDirectory()) {
            ArrayList<String> files = RootShell.listFilesInFolder(folderPath);

            if (files != null && files.size() > 0) {
                for (String file : files) {
                    if (file.contains(APK_EXTENSION) && !installedFiles.contains(file)) {
                        installedFiles.add(file);
                        addFileToMap(folderPath + file);
                    }
                }
            }
        }
    }

    private void addFileToMap(String filePath) {
        AndroidManifest manifest = getManifestFromFile(filePath);
        if (manifest != null && manifest.getPackageName() != null && !manifest.getPackageName().isEmpty()
                && !installedPackages.containsKey(manifest.getPackageName())) {
            installedPackages.put(manifest.getPackageName(), new InstalledPackageInfo(filePath, manifest));
            Log.i("installedPackage", filePath + " is launcher " + (manifest.isLauncher() ? "TRUE" : "FALSE"));
        }
    }

    private AndroidManifest getManifestFromFile(String pathToFile){
        String manifestXmlString = parseManifestFile(pathToFile);
        AndroidManifest androidManifest = new XmlHandler<AndroidManifest>().parse(AndroidManifest.class, manifestXmlString);
        if(androidManifest != null) {
            androidManifest.setLauncher(hasHomeScreenSpecificCategories(androidManifest));
            return androidManifest;
        }else{
            return null;
        }
    }


    public String parseManifestFile(String path) {
        try {
            JarFile jf = new JarFile(path);
            InputStream is = jf.getInputStream(jf.getEntry(ANDROID_MANIFEST_FILE));
            byte[] buf = new byte[is.available()];
            int br = is.read(buf);
            String manifestXml = AndroidXMLDecompress.decompressXML(buf);
            is.close();

            return manifestXml;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean hasHomeScreenSpecificCategories(String manifestXml){
        return manifestXml != null && !manifestXml.isEmpty() && manifestXml.contains(Intent.CATEGORY_HOME) && manifestXml.contains(Intent.CATEGORY_DEFAULT);
    }

    private boolean hasHomeScreenSpecificCategories(AndroidManifest manifest){
        // if activity has these categories, than it's a possible launcher application
        boolean hasHomeCategory;
        boolean hasDefaultCategory;

        if (manifest.getApplication() != null && manifest.getApplication().getActivities() != null) {
            Iterator<ActivityEntry> activityEntryIterator = manifest.getApplication().getActivities().listIterator();
            while(activityEntryIterator.hasNext()){
                ActivityEntry activityEntry = activityEntryIterator.next();
                if(activityEntry != null && activityEntry.getIntentFilter() != null){
                    Iterator<IntentFilterEntry> intentFilterEntryIterator = activityEntry.getIntentFilter().listIterator();
                    while(intentFilterEntryIterator.hasNext()){
                        IntentFilterEntry intentFilterEntry = intentFilterEntryIterator.next();
                        if(intentFilterEntry != null && intentFilterEntry.getCategories() != null){
                            hasHomeCategory = false;
                            hasDefaultCategory = false;

                            Iterator<IntentCategory> categoryIterator = intentFilterEntry.getCategories().listIterator();
                            while(categoryIterator.hasNext()){
                                IntentCategory category = categoryIterator.next();
                                if (category.getName() != null) {
                                    if (category.getName().equals(Intent.CATEGORY_HOME)) {
                                        hasHomeCategory = true;
                                    } else if (category.getName().equals(Intent.CATEGORY_DEFAULT)) {
                                        hasDefaultCategory = true;
                                    }
                                }
                            }

                            if(hasHomeCategory && hasDefaultCategory){
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
