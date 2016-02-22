package y3k.tpemaptest;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final float minZoomLevel = 15f;
    ArrayList<Marker> mapMarkers = new ArrayList<>();

    private GoogleMap googleMap;
    //    private ClusterManager<ClusterItem> mapItemClusterManager;
//    private class MapClusterItem implements ClusterItem{
//        private LatLng latLng;
//        public MapClusterItem(final LatLng latLng){
//            this.latLng = latLng;
//        }
//        @Override
//        public LatLng getPosition() {
//            return this.latLng;
//        }
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                boolean markersVisibility = false;
                if(cameraPosition.zoom>minZoomLevel){
//                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition.target, minZoomLevel));
                    markersVisibility = true;
                }
                for(Marker marker : mapMarkers){
                    marker.setVisible(markersVisibility);
                }
            }
        });
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.0449695d, 121.5087531d), minZoomLevel));
//        this.mapItemClusterManager = new ClusterManager<>(this,this.googleMap);
//        this.googleMap.setOnCameraChangeListener(this.mapItemClusterManager);
//        this.googleMap.setOnMarkerClickListener(this.mapItemClusterManager);
//        this.googleMap.setOnInfoWindowClickListener(this.mapItemClusterManager);
//        this.mapItemClusterManager.setRenderer(new DefaultClusterRenderer<ClusterItem>(this,this.googleMap,this.mapItemClusterManager){
//            @Override
//            protected boolean shouldRenderAsCluster(Cluster<ClusterItem> cluster) {
//                return cluster.getSize()>20;
//            }
//        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    assetReadHandler.sendEmptyMessage(0);
                    BufferedReader busInfoFileBufferReader = new BufferedReader(new InputStreamReader(getAssets().open("GetStopLocation.json")));
                    JSONObject busInfoRawJsonObject = new JSONObject(busInfoFileBufferReader.readLine());
                    Message message = new Message();
                    message.obj = busInfoRawJsonObject;
                    assetReadHandler.sendEmptyMessage(1);
                    busStateJJsonArrayHandler.sendMessage(message);
                } catch (JSONException e){
                    Log.e("y3k", e.getMessage());
                    e.printStackTrace();
                } catch (IOException e1){
                    Log.e("y3k", e1.getMessage());
                    e1.printStackTrace();
                }
            }
        }).start();

//        LatLng sydney = new LatLng(-34, 151);
//        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    Handler assetReadHandler = new Handler(new Handler.Callback(){
        ProgressDialog assetReadProgressDialog;
        @Override
        public boolean handleMessage(Message message) {
            if(message.what==0){
                assetReadProgressDialog = ProgressDialog.show(MapsActivity.this, "Info", "Loading Bus Stops Database...");
            }
            else{
                assetReadProgressDialog.dismiss();
            }
            return true;
        }
    });

    Handler busStateJJsonArrayHandler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message message) {
            try {
                JSONArray busInfoStopsArray = ((JSONObject) message.obj).getJSONArray("BusInfo");
                ProgressDialog addMarkerProgressDialog = ProgressDialog.show(MapsActivity.this, "Info", "Preparing To Add Markers..");
                addMarkerProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                for (int i = 0; i < 10; i++) {
                for (int i = 0; i < busInfoStopsArray.length(); i++) {
                    addMarkerProgressDialog.setMessage("Adding Marker # " + (i + 1));
                    addMarkerProgressDialog.setProgress((int)(100f*(((float)i)/((float)busInfoStopsArray.length()))));
                    Log.d("y3k", "Adding Marker # " + (i + 1));
                    JSONObject stopJsonObject = busInfoStopsArray.getJSONObject(i);
                    LatLng latLng = new LatLng(stopJsonObject.getDouble("lat"), stopJsonObject.getDouble("lon"));
                    MarkerOptions markerOption = new MarkerOptions().position(latLng).title(stopJsonObject.getString("name") + "(" + stopJsonObject.getString("address") + ")");
                    mapMarkers.add(googleMap.addMarker(markerOption));
//                    if(i==busInfoStopsArray.length()-1||i==0){
//                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18f));
//                    }
//                    mapItemClusterManager.addItem(new MapClusterItem(latLng));
                }
//                mapItemClusterManager.onCameraChange(googleMap.getCameraPosition());
                addMarkerProgressDialog.dismiss();
                return true;
            } catch (JSONException e) {
                Log.e("y3k", e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    });
}
