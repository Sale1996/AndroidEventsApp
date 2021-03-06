package rs.ac.uns.ftn.eventsapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.squareup.picasso.Picasso;

import rs.ac.uns.ftn.eventsapp.R;

public class ClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker> {

    private final IconGenerator iconGenerator;
    private final ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;

    public ClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super(context, map, clusterManager);

        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = 200;
        markerHeight = 200;
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        int padding = 20;
        imageView.setPadding(padding, padding, padding, padding);
        iconGenerator.setContentView(imageView);
    }

    @Override
    protected void onBeforeClusterItemRendered(ClusterMarker item, MarkerOptions markerOptions) {
        //Picasso.get().load(item.getImgUri()).placeholder(R.drawable.ic_missing_event_icon).into(imageView);
        if (item.getImgUri() != null && !item.getImgUri().equals("")) {
            Picasso.get().setLoggingEnabled(true);
            if (item.getImgUri().startsWith("http")) {
                Picasso.get().load(Uri.parse(item.getImgUri())).placeholder(R.drawable.ic_missing_event_icon).into(imageView);
            } else {
                Picasso.get().load(Uri.parse(AppDataSingleton.IMAGE_URI + item.getImgUri())).placeholder(R.drawable.ic_missing_event_icon).into(imageView);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_missing_event_icon);
        }

        Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.getTitle());

    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<ClusterMarker> cluster) {
        return false;
    }

}
