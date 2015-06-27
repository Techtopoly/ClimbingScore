package rockscience.climbingscore;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;


public class MainActivity extends Activity {


    EditText txtScore, txtUser, txtClimbId, txtStartTime, txtFinishTime;
    List<Climb> Climbs = new ArrayList<Climb>();
    ListView climbListView;
    DatabaseHandler dbHandler;
    ArrayAdapter<Climb> climbAdapter;
    int longClickedItemIndex;
    // Nfc variables
    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
    public static final String TAG = "NfcDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if NFC is enabled
        mTextView = (TextView) findViewById(R.id.txtNfc);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            //Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            //finish();
            mTextView.setText("This device doesn't support NFC.");
        } else {
            if (!mNfcAdapter.isEnabled()) {
                mTextView.setText("NFC is disabled.");
            } else {
                mTextView.setText(R.string.nfcTag);
            }
        }
        handleIntent(getIntent());

        txtScore = (EditText) findViewById(R.id.txtScore);
        txtUser = (EditText) findViewById(R.id.txtUser);
        txtClimbId = (EditText) findViewById(R.id.txtClimbId);
        txtStartTime = (EditText) findViewById(R.id.txtStartTime);
        txtFinishTime = (EditText) findViewById(R.id.txtFinishTime);
        climbListView = (ListView) findViewById(R.id.listHistory);
        dbHandler = new DatabaseHandler(getApplicationContext());

        registerForContextMenu(climbListView);

        climbListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                longClickedItemIndex = position;
                return false;
            }
        });

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("current");
        tabSpec.setContent(R.id.tabCurrentRoute);
        tabSpec.setIndicator("Current");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("history");
        tabSpec.setContent(R.id.tabUserHistory);
        tabSpec.setIndicator("History");
        tabHost.addTab(tabSpec);

        final Button getTime = (Button) findViewById(R.id.buttonGetTime);
        getTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                GregorianCalendar todayDate = new GregorianCalendar();
                String start = txtStartTime.getText().toString();
                if (!start.isEmpty()) {
                    txtFinishTime.setText(todayDate.getTime().toString());
                } else {
                    txtStartTime.setText(todayDate.getTime().toString());
                }
            }
        });

        final Button addBtn = (Button) findViewById(R.id.buttonAddClimb);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Climb climb = new Climb(dbHandler.getClimbCount(), String.valueOf(txtUser.getText()), String.valueOf(txtScore.getText()), String.valueOf(txtClimbId.getText()), String.valueOf(txtStartTime.getText()), String.valueOf(txtFinishTime.getText()));
                dbHandler.createClimb(climb);
                Climbs.add(climb);
                climbAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), String.valueOf(txtClimbId.getText()) + " has been added to your Climb!", Toast.LENGTH_SHORT).show();
           }
        });

        if (dbHandler.getClimbCount() != 0)
            Climbs.addAll(dbHandler.getAllClimbs());

        populateList();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if ("text/plain".equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void populateList() {
        climbAdapter = new ClimbListAdapter();
        climbListView.setAdapter(climbAdapter);
    }

    private class ClimbListAdapter extends ArrayAdapter<Climb> {
        public ClimbListAdapter() {
            super (MainActivity.this, R.layout.listview_item, Climbs);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.listview_item, parent, false);

            Climb currentClimb = Climbs.get(position);

            TextView name = (TextView) view.findViewById(R.id.climbName);
            name.setText(currentClimb.getName());
            TextView score = (TextView) view.findViewById(R.id.climbScore);
            score.setText(currentClimb.getScore());
            TextView climbid = (TextView) view.findViewById(R.id.climbId);
            climbid.setText(currentClimb.getClimbid());
            TextView start = (TextView) view.findViewById(R.id.startTime);
            start.setText(currentClimb.getStart());
            TextView finish = (TextView) view.findViewById(R.id.finishTime);
            finish.setText(currentClimb.getFinish());

            return view;
        }
    }

    private boolean climbExists(Climb climb) {
        String name = climb.getName();
        int climbCount = Climbs.size();

        for (int i = 0; i < climbCount; i++) {
            if (name.compareToIgnoreCase(Climbs.get(i).getName()) == 0)
                return true;
        }
        return false;
    }

    /*public void getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        GregorianCalendar todayDate = new GregorianCalendar();
        txtStartTime.setText(todayDate.getTime().toString());
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            String UTF8 = "UTF-8";
            String UTF16 = "UTF-16";

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? UTF8 : UTF16;

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);
            }
        }
    }
}


