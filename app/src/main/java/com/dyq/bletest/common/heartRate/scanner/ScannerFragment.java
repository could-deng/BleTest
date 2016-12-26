//package com.dyq.bletest.common.heartRate.scanner;
//
//import android.Manifest;
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.app.Dialog;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.ParcelUuid;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.DialogFragment;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.Button;
//import android.widget.ListView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.dyq.bletest.R;
//import com.dyq.bletest.view.adapter.DeviceListAdapter;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
//import no.nordicsemi.android.support.v18.scanner.ScanCallback;
//import no.nordicsemi.android.support.v18.scanner.ScanFilter;
//import no.nordicsemi.android.support.v18.scanner.ScanResult;
//import no.nordicsemi.android.support.v18.scanner.ScanSettings;
//
///**
// * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter devices with standard BLE Service UUID and devices with custom BLE Service UUID. It contains a
// * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is implemented by activity in order to receive selected device. The scanning will continue to scan
// * for 5 seconds and then stop.
// */
//public class ScannerFragment extends DialogFragment {
//	private final static String TAG = "ScannerFragment";
//
//	private final static String PARAM_UUID = "param_uuid";
//	private final static long SCAN_DURATION = 5000;
//
//	private final static int REQUEST_PERMISSION_REQ_CODE = 34; // any 8-bit number
//
////	private BluetoothAdapter mBluetoothAdapter;
//	private OnDeviceSelectedListener mListener;
//	private DeviceListAdapter mAdapter;
//	private final Handler mHandler = new Handler();
//	private Button mScanButton;
//	private TextView tv_scanner_title;//搜索框的标题栏
//
//	private View mPermissionRationale;
//
//	private ParcelUuid mUuid;
//
//	private boolean mIsScanning = false;
//
//	public static ScannerFragment getInstance(final UUID uuid) {
//		final ScannerFragment fragment = new ScannerFragment();
//
//		final Bundle args = new Bundle();
//		if (uuid != null)
//			args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
//		fragment.setArguments(args);
//		return fragment;
//	}
//
//	/**
//	 * Interface required to be implemented by activity.
//	 */
//	public interface OnDeviceSelectedListener {
//		/**
//		 * Fired when user selected the device.
//		 *
//		 * @param device
//		 *            the device to connect to
//		 * @param name
//		 *            the device name. Unfortunately on some devices {@link BluetoothDevice#getName()} always returns <code>null</code>, f.e. Sony Xperia Z1 (C6903) with Android 4.3. The name has to
//		 *            be parsed manually form the Advertisement packet.
//		 */
//		void onDeviceSelected(final BluetoothDevice device, final String name);
//
//		/**
//		 * Fired when scanner dialog has been cancelled without selecting a device.
//		 */
//		void onDialogCanceled();
//	}
//
//	/**
//	 * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity.
//	 */
//	@Override
//	public void onAttach(final Activity activity) {
//		super.onAttach(activity);
//		try {
//			this.mListener = (OnDeviceSelectedListener) activity;
//		} catch (final ClassCastException e) {
//			throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
//		}
//	}
//
//	@Override
//	public void onCreate(final Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		final Bundle args = getArguments();
//		if (args.containsKey(PARAM_UUID)) {
//			mUuid = args.getParcelable(PARAM_UUID);
//		}
//
////		final BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
////		mBluetoothAdapter = manager.getAdapter();
//	}
//
//	@Override
//	public void onDestroyView() {
//		stopScan();
//		super.onDestroyView();
//	}
//
//	@NonNull
//    @Override
//	public Dialog onCreateDialog(final Bundle savedInstanceState) {
//		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//		final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
//		final ListView listview = (ListView) dialogView.findViewById(android.R.id.list);
//
//		listview.setEmptyView(dialogView.findViewById(android.R.id.empty));
//		listview.setAdapter(mAdapter = new DeviceListAdapter(getActivity()));
//
////		TextView tv = new TextView(getActivity());
////		tv.setBackgroundResource(R.drawable.fitmix_black_bg);
////		tv.setText(R.string.scanner_title);
////		builder.setCustomTitle(tv);
//
////		builder.setTitle(R.string.scanner_title);
//		tv_scanner_title = (TextView) dialogView.findViewById(R.id.tv_scanner_title);
//
//		final AlertDialog dialog = builder.setView(dialogView).create();
//		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
//				stopScan();
//				dialog.dismiss();
//				final ExtendedBluetoothDevice d = (ExtendedBluetoothDevice) mAdapter.getItem(position);
//				mListener.onDeviceSelected(d.device, d.name);
//			}
//		});
//
//		mPermissionRationale = dialogView.findViewById(R.id.permission_rationale); // this is not null only on API23+
//
//		mScanButton = (Button) dialogView.findViewById(R.id.action_cancel);
//		mScanButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (v.getId() == R.id.action_cancel) {
//					if (mIsScanning) {
//						dialog.cancel();
//					} else {
//						if(isBLEEnabled()){
//							startScan();
//						}else{
//							requestBlueTooth();
//						}
//					}
//				}
//			}
//		});
//
////		addBondedDevices();
//		if (savedInstanceState == null)
//			startScan();
//		return dialog;
//	}
//
//	/**
//	 * 是否允许蓝牙4.0权限
//	 * @return
//	 */
//	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
//	private boolean isBLEEnabled() {
//		final BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
//		final BluetoothAdapter adapter = bluetoothManager.getAdapter();
//		return adapter != null && adapter.isEnabled();
//	}
//	/**
//	 * 请求蓝牙权限，去启动蓝牙
//	 */
//	private void requestBlueTooth() {
//		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//		startActivity(enableIntent);
//	}
//
//	@Override
//	public void onCancel(DialogInterface dialog) {
//		super.onCancel(dialog);
//
//		mListener.onDialogCanceled();
//	}
//
//	@Override
//	public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
//		switch (requestCode) {
//			case REQUEST_PERMISSION_REQ_CODE: {
//				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//					// We have been granted the Manifest.permission.ACCESS_COARSE_LOCATION permission. Now we may proceed with scanning.
//					startScan();
//				} else {
//					mPermissionRationale.setVisibility(View.VISIBLE);
//					Toast.makeText(getActivity(), R.string.no_required_permission, Toast.LENGTH_SHORT).show();
//				}
//				break;
//			}
//		}
//	}
//
//	/**
//	 * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out.
//	 * using class ScannerServiceParser
//	 */
//	private void startScan() {
//		// Since Android 6.0 we need to obtain either Manifest.permission.ACCESS_COARSE_LOCATION or Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for
//		// Bluetooth LE devices. This is related to beacons as proximity devices.
//		// On API older than Marshmallow the following code does nothing.
//		if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//			// When user pressed Deny and still wants to use this functionality, show the rationale
//			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) && mPermissionRationale.getVisibility() == View.GONE) {
//				mPermissionRationale.setVisibility(View.VISIBLE);
//				return;
//			}
//
//			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
//			return;
//		}
//
//		// Hide the rationale message, we don't need it anymore.
//		if (mPermissionRationale != null)
//			mPermissionRationale.setVisibility(View.GONE);
//
//		mAdapter.clearDevices();
//		mScanButton.setText(R.string.scanner_action_cancel);
//		tv_scanner_title.setText(getResources().getString(R.string.scanner_equipments_searching));
//
//
//		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
//		final ScanSettings settings = new ScanSettings.Builder()
//				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000).setUseHardwareBatchingIfSupported(false).build();
//		final List<ScanFilter> filters = new ArrayList<>();
//		filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
//		scanner.startScan(filters, settings, scanCallback);
//
//		mIsScanning = true;
//		mHandler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				if (mIsScanning) {
//					stopScan();
//				}
//			}
//		}, SCAN_DURATION);
//	}
//
//	/**
//	 * Stop scan if user tap Cancel button
//	 */
//	private void stopScan() {
//		if (mIsScanning) {
//			mScanButton.setText(R.string.scanner_action_scan);
//			tv_scanner_title.setText(getResources().getString(R.string.scanner_select_heart_rate_equipment));
//
//			final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
//			scanner.stopScan(scanCallback);
//
//			mIsScanning = false;
//		}
//	}
//
//	private ScanCallback scanCallback = new ScanCallback() {
//		@Override
//		public void onScanResult(final int callbackType, final ScanResult result) {
//			// do nothing
//		}
//
//		@Override
//		public void onBatchScanResults(final List<ScanResult> results) {
//			mAdapter.update(results);
//		}
//
//		@Override
//		public void onScanFailed(final int errorCode) {
//			// should never be called
//		}
//	};
//
////	private void addBondedDevices() {
////		final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
////		mAdapter.addBondedDevices(devices);
////	}
//}
