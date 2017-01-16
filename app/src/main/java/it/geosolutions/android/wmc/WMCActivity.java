package it.geosolutions.android.wmc;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.util.regex.Pattern;

import it.geosolutions.android.wmc.model.Configuration;
import it.geosolutions.android.wmc.util.ConfigIO;

/**
 * Activity that contains a WMC form fragment
 *
 * it handles the menu events and if necessary requests permissions
 */

public class WMCActivity extends AppCompatActivity  {

    private final static int INTENT_SELECT_FILE = 111;
    private final static int INTENT_KITKAT_SELECT_TARGET = 113;

    protected static final byte PERMISSION_REQUEST = 122;

    private boolean fileSelection = false;
    private boolean folderSelection = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

    }

    @Override
    public void onBackPressed() {

        boolean destroy = true;

        /**
         * when connected ask the user if to disconnect or cancel this back button click
         */

        if(getFormFragment().isConnected()) {

            //don't destroy now
            destroy = false;
            //ask user if to disconnect
            getFormFragment().showAskForDisconnectDialog(WMCActivity.this, new WMCForm.OnDisconnectListener() {
                @Override
                public void onDisconnect() {
                    WMCActivity.super.onBackPressed();
                }
            });

        }
        if(destroy){
            super.onBackPressed();
        }
    }

    /**
     ////////////// ANDROID 6 permissions /////////////////
     */
    /**
     * checks if the permission @param is granted and if not requests it
     * @param permission
     * @return
     */
    public boolean permissionNecessary(final String permission) {

        boolean required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED;

        if (required) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST);
            return true;
        }

        return false;
    }

    /**
     * returns the result of the permission request
     * @param requestCode a requestCode
     * @param permissions the requested permission
     * @param grantResults the result of the user decision
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (PERMISSION_REQUEST == requestCode) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //denied, show a message
                getFormFragment().showAlertDialog(getString(R.string.permission_required));
                return;
            }

            /**
             * if the user did want to read or write to the file system earlier
             * continue here
             */
            if(fileSelection){

                startFileSelectionIntent();
                fileSelection = false;

            }else if(folderSelection){

                startFolderSelectionIntent();
                folderSelection = false;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
        ////////////// MENU /////////////////
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.wmc_menu, menu);

        menu.findItem (R.id.menu_config).getSubMenu().setGroupVisible(R.id.menu_group_read_write, getFormFragment().isConnected());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menu_load_config_from_disk:

                if(!permissionNecessary(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    startFileSelectionIntent();
                }else{
                    fileSelection = true;
                }

                return true;
            case R.id.menu_save_config_to_disk:

                if(getFormFragment().getCurrentConfiguration() == null){
                    getFormFragment().showAlertDialog(getString(R.string.state_no_config_available));
                } else {

                    if (!permissionNecessary(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        startFolderSelectionIntent();

                    } else {
                        folderSelection = true;
                    }
                }
                return true;
            case R.id.menu_read_config_from_device:

                getFormFragment().readConfigFromDevice();

                return true;
            case R.id.menu_write_config_to_device:

                if(getFormFragment().getCurrentConfiguration() == null){
                    getFormFragment().showAlertDialog(getString(R.string.state_no_config_available));
                } else {
                    getFormFragment().sendConfigToDevice();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * starts an intent to select a file
     * uses the Material File Picker library @gradle 'com.nbsp:library:1.1'
     * picked file arrives in onActivityResult
     */
    private void startFileSelectionIntent(){

        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.xml$"));
        intent.putExtra(FilePickerActivity.ARG_START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        startActivityForResult(intent, INTENT_SELECT_FILE);

    }

    /**
     * starts an intent to create a file named @link EXPORT_FILE_NAME within the file system
     * this gives the user the chance to overwrite the last configuration or edit the file name and create another one
     * the result as uri arrives in onActivityResult
     */
    @TargetApi(19)
    private void startFolderSelectionIntent(){

        //let the user selects a file and edit the filename providing the default
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("text/xml")
                .putExtra(Intent.EXTRA_TITLE, WMCForm.EXPORT_FILE_NAME);
        startActivityForResult(intent, INTENT_KITKAT_SELECT_TARGET);

        Toast.makeText(getBaseContext(), getString(R.string.intent_select_target), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == INTENT_SELECT_FILE && resultCode == RESULT_OK && resultData != null) {

            String filePath = resultData.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            final Pair<Configuration, String> readResult = ConfigIO.readAndValidateConfig(getBaseContext(), filePath);

            if(readResult.first != null){
                //success
                getFormFragment().applyNewConfig(readResult.first);
            }
            //report result
            getFormFragment().reportStatus(readResult.second);


        }else if(requestCode == INTENT_KITKAT_SELECT_TARGET && resultCode == RESULT_OK && resultData != null){

            final Uri uri = resultData.getData();

            boolean success = ConfigIO.writeConfigToDisk(uri, getContentResolver(), getFormFragment().getCurrentConfiguration());

            if(success){
                getFormFragment().reportStatus(R.string.state_write_success);
            }else{
                getFormFragment().reportStatus(R.string.state_write_failure);
            }

        }
    }

    public WMCForm getFormFragment() {

        return ((WMCForm) getSupportFragmentManager().findFragmentById(R.id.wmc_fragment));
    }
}
