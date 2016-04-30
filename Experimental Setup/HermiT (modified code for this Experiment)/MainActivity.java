package com.example.hermitowlapi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.derivo.sparqldlapi.Query;
import de.derivo.sparqldlapi.QueryEngine;
import de.derivo.sparqldlapi.QueryResult;
import de.derivo.sparqldlapi.exceptions.QueryEngineException;
import de.derivo.sparqldlapi.exceptions.QueryParserException;
import com.example.hermitowlapi.R;

/**
 * Class MainActivity extends the Activity class and its purpose is to create
 * the HermiT reasoner which is capable to perform the tasks it has been
 * assigned. It also is capable of Measuring the power it drained as well as
 * record the results into different files.
 * 
 * @author Edgaras Valincius
 * @version 1.0
 * @since 2015-02-03
 * @modified by Isa Guclu, 2016-04-30
 * 
 */
public class MainActivity extends ActionBarActivity {

	private GridLayout layout;
	private ProgressDialog progressDialog;
	private Timer timer;
	private float draw;
	private float CurrentInitial, CurrentEnd;
	private float VoltageInitial, VoltageEnd;
	private int mvoltage;
	private float drained, Reasonerdrained, OntologyLoaderDrained;
	private float watts, ReasonerdrainedWatts, OntologyLoaderDrainedWatts;
	private Date Begining, AfterLoading, TheEnd;
	private String datasetFileName, queryName, ontologyName, folder;
	
	/*
	 * private long startCountingTime; private long stopCountingTime;
	 */
	
	/**
	 * onCreate is used to initialize activity. This method launches the
	 * AsyncTask class that allows to use background operations.Method also gets
	 * the extras from the intent.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent myIntent = getIntent();
		datasetFileName = myIntent.getStringExtra("ontologyFile");
		queryName = myIntent.getStringExtra("queryName");
		ontologyName = myIntent.getStringExtra("ontologyName");
		if (datasetFileName == null) {
			System.out.println("CLOSED. Dataset Empty");
			// Thread is used to hold the activity, before closing it, so
			// the Toast have enough time to show its message.
			Thread thread = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(3500); // As I am using LENGTH_LONG in
											// Toast
						finish();
						System.exit(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			Toast.makeText(getApplicationContext(), "Launch From The PowerBenchMark app", Toast.LENGTH_LONG).show();
			thread.start();

		} else {
			// instantiate new progress dialog
			progressDialog = new ProgressDialog(this);
			// spinner (wheel) style dialog
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// better yet - use a string resource
			// getString(R.string.your_message)
			progressDialog.setMessage("Please Wait");
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					onBackPressed();
				}
			});

			// display dialog
			progressDialog.show();

			// start async task
			new MyAsyncTaskClass().execute();
		}

	}

	public void setText(String a) {
		TextView batteryInfo = (TextView) findViewById(R.id.textView);
		batteryInfo.setText("Results: " + a.toString());
	}

	/**
	 * Creates Asynchronous tasks on one same UI thread. In this case progress
	 * wheel and the calculations are performed asynchronously.
	 */
	private class MyAsyncTaskClass extends AsyncTask<Void, Void, Void> {
		/**
		 * Method performs a computation on a background thread.
		 */
		@Override
		protected Void doInBackground(Void... params) {

			layout = (GridLayout) findViewById(R.id.layout);
			Collection<View> views = new ArrayList<View>();
			views.add(layout);

			File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					+ File.separator + "datasets");
			File[] listOfFiles = folder.listFiles();

			ArrayList<String> sListFiles = new ArrayList<String>();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					sListFiles.add(listOfFiles[i].getName().toString());
				}
			}

			for (int i = 0; i < sListFiles.size(); i++) {
				System.out.println("File " + sListFiles.get(i));
				try {

					CurrentInitial = -1;
					CurrentEnd = -1;
					VoltageInitial = -1;
					VoltageEnd = -1;

					drained = 0;
					mvoltage = 0;
					watts = 0;
					OntologyLoaderDrained = 0;
					OntologyLoaderDrainedWatts = 0;
					// calls method, which launches an experiment.
					executeQueries(folder, sListFiles.get(i), "Classification");
					Thread.sleep(1000);
				} catch (Exception e) {
					WriteException("NOT WORKED: doInBackground: " + e.toString());
					moveFile(folder.toString(), sListFiles.get(i), "" + folder + File.separator + "HermitError");
				}
			}
			return null;
		}

		/**
		 * Runs on the UI thread after doInBackground.
		 */
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			stop();
			finishWithResult(1);
			finish();
			System.exit(0);
		}
	}

	public void executeQueries(File pFolder, String pOntologyName, String pQuery) {
		folder = pFolder.toString();
		queryName = pQuery;
		ontologyName = pOntologyName;
		String dStartTime = StringOfNow();

		// Starts timer that calculates the mAh drained
		start();
		// calls method to start inspecting voltage.
		getVoltage();

		Begining = new Date();
		File file = new File(pFolder + File.separator + pOntologyName);
		org.semanticweb.HermiT.Reasoner hermit = null;

		OWLOntology ont = null;
		OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();

		try {
			// startCountingTime = System.currentTimeMillis();
			ont = ontManager.loadOntologyFromOntologyDocument(IRI.create(file));
			hermit = new Reasoner(ont);// factory.createReasoner(ont);

			if (ont != null) {

				QueryEngine queryEng = QueryEngine.create(ontManager, hermit);
				String dQuery = "SELECT * WHERE { SubClassOf(?x,?y) }";

				// "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
				// + "select * where {?X rdfs:subClassOf ?Y}";

				OntologyLoaderDrained = drained;
				OntologyLoaderDrainedWatts = watts;

				AfterLoading = new Date(); 
				
				Query query = Query.create(dQuery);
				QueryResult result = queryEng.execute(query);
				System.out.println(result);

				// records how much mAh reasoner drained.
				Reasonerdrained = drained - OntologyLoaderDrained;
				// records how much watts reasoner drained
				ReasonerdrainedWatts = watts - OntologyLoaderDrainedWatts;

				TheEnd = new Date(); 

				DecimalFormat df = new DecimalFormat("#.#####"); // df.format(0.9123);
				double dTotal = (double) DateDiffMsec(Begining, TheEnd) / 1000;
				double dLoading = (double) DateDiffMsec(Begining, AfterLoading) / 1000;
				double dReasoning = (double) DateDiffMsec(AfterLoading, TheEnd) / 1000;

				write("Total.Hermit", "HermitLog", ontologyName + ":" + dStartTime + ": started:" + StringOfNow()
						+ ": ended:" + OntologyLoaderDrained + ": mAh. Load:" + OntologyLoaderDrainedWatts
						+ ": Ws. Load:" + df.format(dLoading) + ": sec Load :" + Reasonerdrained + ": mAh. Reasoning:"
						+ ReasonerdrainedWatts + ": Ws. Reasoning:" + df.format(dReasoning) + ": sec Reasoning :"
						+ drained + ": mAh. Total :" + watts + ": Ws. Total :" + df.format(dTotal) + ": sec Total:"
						+ CurrentInitial + ": mAh. CurrentInital :" + CurrentEnd + ": mAh. CurrentEnd :"
						+ VoltageInitial + ": Ws. VoltageInitial :" + VoltageEnd + ": Ws. VoltageEnd :");

				/* RESULT */
				// write(ontologyName + ".Hermit", "HermitResult", "" + result);
				
				moveFile(pFolder.toString(), pOntologyName, "" + pFolder + File.separator + "HermitDone");
			
			} else {
				WriteException("ONTOLOGY IS NULL.");
				moveFile(pFolder.toString(), pOntologyName, "" + pFolder + File.separator + "HermitError");
			}

		} catch (Exception e) {
			WriteException(e.toString());
			moveFile(pFolder.toString(), pOntologyName, "" + pFolder + File.separator + "HermitError");
			e.printStackTrace();
		}

	}

	/**
	 * Battery method bat() reads the battery information and return the current
	 * flow of the battery.
	 * 
	 * @return float draw that is current in mA flowing from the battery at the
	 *         moment.
	 */
	public float bat() {
		BatteryManager mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
		Long energy = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
		float currentdraw = energy;
		draw = currentdraw;
		return draw;
	}

	/**
	 * Starts timer that registers current flow in mA of battery and transforms
	 * it to mAh.
	 */
	public void start() {
		if (timer != null) {
			return;
		}
		timer = new Timer();
		timer.schedule(new TimerTask() {
			/**
			 * Timer calls method run every second. Within run, power
			 * consumption is calculated. Method invokes
			 */
			public void run() {
				float current = bat() / 1000 ; // / 1000 20160305 To convert micro to mili, in Xperia
				current = (float) current / 10 ; // FOR 100 MSEC INTERVALS
				/**
				 * 3300s instead 3600s because after calculations there were
				 * some error rate determined and divided from 3300 covers the
				 * loss of data that was missed to be recorded. Calculated by
				 * measuring amount of current drained per 1% and finding the
				 * constant that derives 31mah.
				 */

				drained = drained + ((float) current / 3300);
				if (CurrentInitial == -1 && current != 0) {
					CurrentInitial = current;
				}
				CurrentEnd = current;
				/**
				 * Watts drained were calculated by following formula W=I*V
				 * (watt= current * voltage). Since voltage was measured in
				 * miliVolts, the equation had to be divided from 1000 to get
				 * the SI units. In case below, it was also multiplied by time,
				 * so was converted back to Watts instead of watt/hours.
				 */
				
				mvoltage = getVoltage();
				if (current == 0){
					write(ontologyName, "HermitCurrent", ontologyName + "\n" + "Current Couldnot be retrieved");
				}
				if (mvoltage == -1){
					write(ontologyName, "HermitVoltage", ontologyName + "\n" + "Voltage Couldnot be retrieved");
				}
				if (VoltageInitial == -1 && mvoltage != -1){   //mvoltage != 0) {
					VoltageInitial = mvoltage;
				}

				VoltageEnd = mvoltage;
				
				watts = (float) ((float) ((float) drained * (float) mvoltage / 1000) * 3.6);
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Date RightNow = new Date();
						float timeElapsed = (float) ((float) DateDiffMsec(Begining, RightNow) / 1000.0);
						((TextView) findViewById(R.id.textView)).setText(
								"Hermit REASONER:\n" + "Reasoning task: " + queryName + " \n" + "Ontology size : "
										+ ontologyName + "\n" + "Capacity drained = " + drained + "mAh \n"
										+ "Time elapsed : " + timeElapsed + "s" + "\nPower consumed: " + watts + "Ws");
						// This if ABORTS the reasoning task because it took too long,						
						// if (timeElapsed > 300 || drained > 45) {
						if (timeElapsed > 110) {
							moveFile(folder, ontologyName, "" + folder + File.separator + "HermitError");
							quiteAnApp(1);
						}
					}
				});
			}
		}, 0, 100);
	}

	/**
	 * Stops the previously launched Timer.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}

	/**
	 * Sends results to intent activity (PowerBenchMark app that was called
	 * from) to send the information that it finished its task.
	 */
	private void finishWithResult(int a) {

		Bundle conData = new Bundle();
		conData.putInt("results", a);
		Intent intent = new Intent();
		intent.putExtras(conData);
		setResult(RESULT_OK, intent);
	}

	/**
	 * Closes the app. Is called when reasoner encounters an error and is
	 * manually called for closing. Records the power consumption it drained.
	 */
	public void quiteAnApp(int a) {
		Reasonerdrained = drained - OntologyLoaderDrained;
		ReasonerdrainedWatts = watts - OntologyLoaderDrainedWatts;

		Date RightNow = new Date();
		float timeElapsed = (float) ((float) DateDiffMsec(Begining, RightNow) / 1000.0);

		write("Hermit." + StringOfNow() + "." + ontologyName + "_Abort", "HermitLog",
				"________ABORTED____________\n" + "Hermit REASONER:\n" + "Reasoning task: " + queryName + "\n"
						+ "Ontology size : " + ontologyName + "\n" + "Reasoning task drained: " + Reasonerdrained
						+ "mAh" + "\n" + "Ontology loader drained: " + OntologyLoaderDrained + "mAh" + "\n"
						+ "Hermit drained total: " + drained + "mAh" + "\n" + "Time elapsed: " + timeElapsed + "s\n"
						+ "Power consumed: " + watts + "Ws" + "\n________________________");
		write("justdata_Abort_", "HermitLog", "" + Reasonerdrained);
		write("PowerReasoner_Abort_", "HermitLog", "" + ReasonerdrainedWatts);
		write("Hermit_Abort." + StringOfNow(), "HermitResult", "Results Aborted ");
		write("ReasonerTime_Abort_", "HermitLog", "" + timeElapsed);
		progressDialog.dismiss();
		stop();
		finishWithResult(a);
		finish();
		System.exit(0);
	}

	/**
	 * Method records voltage. To do that it has to register the
	 * BroadcastReceiver, and every time the state of voltage changes, it
	 * records the resigns the mvoltage variable with the latest voltage
	 * measured in miliVolts.
	 */
	public int getVoltage() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent b = this.registerReceiver(null, ifilter);
		return b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
	}

	/**
	 * Method created the pop up dialog asking if user wants really quite and
	 * application.
	 */
	@Override
	public void onBackPressed() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.customexit);
		dialog.setTitle("HermiT");
		// set the custom dialog components - text, image and button
		TextView text = (TextView) dialog.findViewById(R.id.text);
		text.setText("Are you sure you want");
		TextView text2 = (TextView) dialog.findViewById(R.id.text2);
		text2.setText("to CANCEL reasoning?");
		ImageView image = (ImageView) dialog.findViewById(R.id.image);
		image.setImageResource(R.drawable.cancel);
		Button dialogButton = (Button) dialog.findViewById(R.id.btnok);
		Button dialogButton2 = (Button) dialog.findViewById(R.id.btncancel);
		// if button is clicked, close the custom dialog
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quiteAnApp(-1);
				dialog.dismiss();
			}
		});

		dialogButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				progressDialog.show();
				dialog.dismiss();

			}
		});

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				progressDialog.show();
			}
		});

		dialog.show();
	}

	public void write(String fileName, String folderName, String fcontent) {
		String dFilename = "";
		String temp = "";
		BufferedWriter writer = null;
		try {
			if (folderName.equalsIgnoreCase("")) {
				dFilename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + fileName + ".txt";
				temp = read(dFilename);
			} else {
				File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + folderName);
				if (!directory.exists()) {
					directory.mkdir();
				}
				dFilename = directory.toString() + File.separator + fileName + ".txt";
				temp = read(dFilename);
			}

			File logFile = new File(dFilename);
			// System.out.println(logFile.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(logFile));
			writer.write(temp + fcontent);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close(); // Close the writer regardless of what
								// happens...
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String read(String fname) {
		BufferedReader br = null;
		String response = null;
		try {
			StringBuffer output = new StringBuffer();
			// String fpath = "storage/emulated/0/Download/" + fname + ".txt";
			String fpath = fname;
			br = new BufferedReader(new FileReader(fpath));
			String line = "";
			while ((line = br.readLine()) != null) {
				output.append(line + "\n");
			}
			response = output.toString();
			br.close();
		} catch (Exception e) { // e.printStackTrace();
			return "";
		}
		return response;
	}

	public void WriteException(String ErrContent) {
		write("Hermit." + StringOfNow() + "." + ontologyName, "HermitError", ontologyName + "\n" + ErrContent);
	}

	public String StringOfNow() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
		Date date = new Date();
		String dNow = dateFormat.format(date);
		// System.out.println(dNow);
		return dNow;
	}

	public static long DateDiffMsec(Date date1, Date date2) {
		long diffInMillies = date2.getTime() - date1.getTime();
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	private void moveFile(String inputPath, String inputFile, String outputPath) {

		InputStream in = null;
		OutputStream out = null;
		try {
			// create output directory if it doesn't exist
			File dir = new File(outputPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			in = new FileInputStream(inputPath + File.separator + inputFile);
			out = new FileOutputStream(outputPath + File.separator + inputFile);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			// write the output file
			out.flush();
			out.close();
			out = null;

			// delete the original file
			new File(inputPath + File.separator + inputFile).delete();

		} catch (Exception e) {
			WriteException("ERROR in COPYING FILE: " + e.toString());
		}

	}

}
