package tr.tech.adaptui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
/*import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;*/
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import tr.tech.adaptui.R;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.sparql.OWLReasonerSPARQLEngine;
import org.semanticweb.sparql.arq.OWLOntologyDataSet;
import org.semanticweb.sparql.arq.TrOWLOntologyGraph;
import org.semanticweb.sparql.bgpevaluation.monitor.MinimalPrintingMonitor;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.function.library.date;

/**
 * Class MainActivity extends the Activity class and its purpose is to create
 * the TrOWL reasoner which is capable to perform the tasks it has been
 * assigned. It also is capable of Measuring the power it drained as well as
 * record the results into different files.
 * 
 * @author Isa Guclu (Code of Edgaras Valincius is utilized)
 * @version 1.0
 * @since 2016-02-16
 */
@SuppressLint("SdCardPath")
public class ActivityExample extends Activity {

	private GridLayout layout;
	private ProgressDialog progressDialog;
	private Timer timer;
	private float draw;
	private float CurrentInitial, CurrentEnd;
	private float VoltageInitial, VoltageEnd;
	private float mvoltage;
	private float drained, Reasonerdrained, OntologyLoaderDrained;
	private float watts, ReasonerdrainedWatts, OntologyLoaderDrainedWatts;
	private Date Begining, AfterLoading, TheEnd;
	private String datasetFileName, queryName, ontologyName, folder;

	/**
	 * onCreate is used to initialize activity. This method launches the
	 * AsyncTask class that allows to use background operations.Method also gets
	 * the extras from the intent.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent myIntent = getIntent(); // gets the previously created intent
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

			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
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

			/*
			for (int i = 0; i < 10000; i++) {
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
					executeQueries(folder, "approximated_00518.owl_RDFXML.owl", "Classification");
					Thread.sleep(1000);
				} catch (Exception e) {
					WriteException("NOT WORKED: doInBackground: " + e.toString());
				}
			}
			*/
			
			ArrayList<String> sListFiles = new ArrayList<String>();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					sListFiles.add(listOfFiles[i].getName().toString());
				}
			}

			ShuffleStrings(sListFiles);
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
					moveFile(folder.toString(), sListFiles.get(i), "" + folder + File.separator + "TrOWLError");
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

		// WRITE START LOG
		String dStartFile = "TrOWL." + StringOfNow() + "." + ontologyName;
		write(dStartFile, "TrOWLStart", ontologyName + "\n"  );
		
		folder = pFolder.toString();
		queryName = pQuery;
		ontologyName = pOntologyName;
		String dStartTime = StringOfNow();
		// Starts timer that calculates the mAh drained
		start();
		// calls method to start inspecting voltage.
		getVoltage();

		Begining = new Date(); 
		String sfile = pFolder + File.separator + pOntologyName;
		InputStream in = null;
		OWLOntology ontology = null;
		OWLOntologyDataSet dataset = null;
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		try {
			in = new FileInputStream(sfile); // System.out.println("OK - in.");
			ontology = manager.loadOntologyFromOntologyDocument(in);// physicalIRI);
			dataset = new OWLOntologyDataSet(ontology, new HashMap<String, OWLOntology>());

			String dQuery = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " select * where {?X rdfs:subClassOf ?Y}";

			// records how much loader drained of a battery
			OntologyLoaderDrained = drained;
			OntologyLoaderDrainedWatts = watts;

			AfterLoading = new Date();
			/*
			 * write("LoaderTime", "log", ontologyName + ":" + timeElapsed +
			 * " sec.");
			 */

			Query query = QueryFactory.create(dQuery);// System.out.println(dQuery);
			// construct SPARQL engine
			OWLReasonerSPARQLEngine sparqlEngine = new OWLReasonerSPARQLEngine(new MinimalPrintingMonitor());
			// records how much loader drained of a battery

			com.hp.hpl.jena.query.ResultSet results = sparqlEngine.execQuery(query, dataset);
			// converts results to the string
			ByteArrayOutputStream baos = null;
			String s = "";
			try {
				baos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(baos);
				ResultSetFormatter.out(ps, results, query);
				s = new String(baos.toByteArray(), "UTF-8");
			} catch (Exception e1) {
				WriteException(e1.toString());
			}

			// records how much mAh reasoner drained.
			Reasonerdrained = drained - OntologyLoaderDrained;
			// records how much watts reasoner drained
			ReasonerdrainedWatts = watts - OntologyLoaderDrainedWatts;
			TheEnd = new Date(); 

			DecimalFormat df = new DecimalFormat("#.#####"); // df.format(0.9123);
			double dTotal = (double) DateDiffMsec(Begining, TheEnd) / 1000;
			double dLoading = (double) DateDiffMsec(Begining, AfterLoading) / 1000;
			double dReasoning = (double) DateDiffMsec(AfterLoading, TheEnd) / 1000;

			write("TrOWL.Total", "TrOWLLog",
					ontologyName + ":" + dStartTime + ": started:" + StringOfNow() + ": ended:" + OntologyLoaderDrained
							+ ": mAh. Load:" + OntologyLoaderDrainedWatts + ": Ws. Load:" + df.format(dLoading)
							+ ": sec Load :" + Reasonerdrained + ": mAh. Reasoning:" + ReasonerdrainedWatts
							+ ": Ws. Reasoning:" + df.format(dReasoning) + ": sec Reasoning :" + drained
							+ ": mAh. Total :" + watts + ": Ws. Total :" + df.format(dTotal) + ": sec Total:"
							+ CurrentInitial + ": mAh. CurrentInital :" + CurrentEnd + ": mAh. CurrentEnd :"
							+ VoltageInitial + ": Ws. VoltageInitial :" + VoltageEnd + ": Ws. VoltageEnd :");

			/* RESULT */
			// write(ontologyName + ".TrOWL", "Result", "" + s);
			
			in.close();
			moveFile(pFolder.toString(), pOntologyName, "" + pFolder + File.separator + "TrOWLDone");
			
			// DELETE START LOG
			new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator 
					+ "TrOWLStart" + File.separator  + dStartFile + ".txt").delete();

		} catch (Exception E) {
			try {
				in.close();
			} catch (Exception e) {
				WriteException(e.toString());
			}
			WriteException(E.toString());
			moveFile(pFolder.toString(), pOntologyName, "" + pFolder + File.separator + "TrOWLError");
			// quiteAnApp(1);
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
				float current = (float) bat() / 1000  ; // (/ 1000) to convert micro to mili used with Xperia
				current = (float) current / 10 ; // FOR 100 MSEC INTERVALS
				/**
				 * 3300s instead 3600s because after calculations there were
				 * some error rate determined and divided from 3300 covers the
				 * loss of data that was missed to be recorded. Calculated by
				 * measuring amount of current drained per 1% and finding the
				 * constant that derives 31mah.
				 */

				drained = drained + ((float) current / 3300);
				if (CurrentInitial == -1) {
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
					write(ontologyName, "TrOWLCurrent", ontologyName + "\n" + "Current Couldnot be retrieved");
				}
				if (mvoltage == -1){
					write(ontologyName, "TrOWLVoltage", ontologyName + "\n" + "Voltage Couldnot be retrieved");
				}
				if (VoltageInitial == -1){  
					VoltageInitial = mvoltage;
				}

				VoltageEnd = mvoltage;

				// System.out.println("mvoltage : " + mvoltage);
				watts = (float) ((float) ((float) drained * (float) mvoltage / 1000) * 3.6);
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						Date RightNow = new Date();
						float timeElapsed = (float) ((float) DateDiffMsec(Begining, RightNow) / 1000.0);
						((TextView) findViewById(R.id.textView)).setText(
								"TrOWL REASONER:\n" + "Reasoning task: " + queryName + " \n" + "Ontology name : "
										+ ontologyName + "\n" + "Capacity drained = " + drained + "mAh \n"
										+ "Time elapsed : " + timeElapsed + "s" + "\nPower consumed: " + watts + "Ws");
						// This if ABORTS the reasoning task because it took too
						// long,
						// if (timeElapsed > 300 || drained > 45) {
						if (timeElapsed > 110) {
							moveFile(folder, ontologyName, "" + folder + File.separator + "TrOWLError");
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

		write("TrOwl." + StringOfNow() + "." + ontologyName + "_Abort", "TrOWLLog",
				"________ABORTED____________\n" + "TrOWL REASONER:\n" + "Reasoning task: " + queryName + "\n"
						+ "Ontology size : " + ontologyName + "\n" + "Reasoning task drained: " + Reasonerdrained
						+ "mAh" + "\n" + "Ontology loader drained: " + OntologyLoaderDrained + "mAh" + "\n"
						+ "TrOWL drained total: " + drained + "mAh" + "\n" + "Time elapsed: " + timeElapsed + "s\n"
						+ "Power consumed: " + watts + "Ws" + "\n________________________");
		
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
		dialog.setTitle("TrOWL");
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
		write("TrOWL." + StringOfNow() + "." + ontologyName, "TrOWLError", ontologyName + "\n" + ErrContent);
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

	
	public static String ShuffleStrings(ArrayList<String> pListFiles) {
		long seed = System.nanoTime();
		Collections.shuffle(pListFiles, new Random(seed));
		return "ok";
	}

}