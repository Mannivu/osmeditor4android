package de.blau.android.layer.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.DisableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;

public class MapOverlay extends MapViewLayer implements ExtentInterface, DisableInterface, ClickableInterface {

    /** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */
    private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;

    private static final String DEBUG_TAG = "tasks";

    private Bitmap  cachedIconClosed;
    private float   w2closed        = 0f;
    private float   h2closed        = 0f;
    private Bitmap  cachedIconChangedClosed;
    private float   w2changedClosed = 0f;
    private float   h2changedClosed = 0f;
    private Bitmap  cachedIconOpen;
    private float   w2open          = 0f;
    private float   h2open          = 0f;
    private Bitmap  cachedIconChanged;
    private float   w2changed       = 0f;
    private float   h2changed       = 0f;
    private boolean enabled         = false;

    /** Map this is an overlay of. */
    private final Map map;

    /** Bugs visible on the overlay. */
    private TaskStorage tasks = App.getTaskStorage();

    /**
     * Construct a new layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(final Map map) {
        this.map = map;
    }

    @Override
    public boolean isReadyToDraw() {
        enabled = map.getPrefs().areBugsEnabled();
        return enabled;
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (isVisible && enabled) {

            // the idea is to have the circles a bit bigger when zoomed in, not so
            // big when zoomed out
            // currently we don't adjust the icon size for density final float radius = Density.dpToPx(1.0f +
            // osmv.getZoomLevel() / 2.0f);
            ViewBox bb = osmv.getViewBox();

            //
            int w = map.getWidth();
            int h = map.getHeight();
            List<Task> taskList = tasks.getTasks(bb);
            if (taskList != null) {
                Set<String> taskFilter = map.getPrefs().taskFilter();
                for (Task t : taskList) {
                    // filter
                    if (!taskFilter.contains(t.bugFilterKey())) {
                        continue;
                    }
                    float x = GeoMath.lonE7ToX(w, bb, t.getLon());
                    float y = GeoMath.latE7ToY(h, w, bb, t.getLat());

                    if (t.isClosed() && t.hasBeenChanged()) {
                        if (cachedIconChangedClosed == null) {
                            cachedIconChangedClosed = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_changed_closed);
                            w2changedClosed = cachedIconChangedClosed.getWidth() / 2f;
                            h2changedClosed = cachedIconChangedClosed.getHeight() / 2f;
                        }
                        c.drawBitmap(cachedIconChangedClosed, x - w2changedClosed, y - h2changedClosed, null);
                    } else if (t.isClosed()) {
                        if (cachedIconClosed == null) {
                            cachedIconClosed = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_closed);
                            w2closed = cachedIconClosed.getWidth() / 2f;
                            h2closed = cachedIconClosed.getHeight() / 2f;
                        }
                        c.drawBitmap(cachedIconClosed, x - w2closed, y - h2closed, null);
                    } else if (t.isNew() || t.hasBeenChanged()) {
                        if (cachedIconChanged == null) {
                            cachedIconChanged = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_changed);
                            w2changed = cachedIconChanged.getWidth() / 2f;
                            h2changed = cachedIconChanged.getHeight() / 2f;
                        }
                        c.drawBitmap(cachedIconChanged, x - w2changed, y - h2changed, null);
                    } else {
                        if (cachedIconOpen == null) {
                            cachedIconOpen = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_open);
                            w2open = cachedIconOpen.getWidth() / 2f;
                            h2open = cachedIconOpen.getHeight() / 2f;
                        }
                        c.drawBitmap(cachedIconOpen, x - w2open, y - h2open, null);
                    }
                }
            }
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public List<Task> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Task> result = new ArrayList<>();
        if (map.getPrefs().areBugsEnabled()) {
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
            List<Task> taskList = tasks.getTasks(viewBox);
            if (taskList != null) {
                Set<String> taskFilter = map.getPrefs().taskFilter();
                for (Task t : taskList) {
                    // filter
                    if (!taskFilter.contains(t.bugFilterKey())) {
                        continue;
                    }
                    int lat = t.getLat();
                    int lon = t.getLon();
                    float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
                    float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
                    if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
                        if (Math.hypot(differenceX, differenceY) <= tolerance) {
                            result.add(t);
                        }
                    }
                }
            }
            // For debugging the OSB editor when the OSB site is down:
            // result.add(new Bug(GeoMath.yToLatE7(map.getHeight(), viewBox, y), GeoMath.xToLonE7(map.getWidth(),
            // viewBox, x), true));
        }
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_tasks);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        List<BoundingBox> boxes = App.getTaskStorage().getBoundingBoxes();
        if (boxes != null) {
            return BoundingBox.union(new ArrayList<>(boxes));
        }
        return null;
    }

    @Override
    public void disable(Context context) {
        Preferences prefs = new Preferences(context);
        prefs.setBugsEnabled(false);
    }

    @Override
    public void onSelected(FragmentActivity activity, Object object) {
        if (!(object instanceof Task)) {
            Log.e(DEBUG_TAG, "Wrong object for " + getName() + " " + object.getClass().getName());
            return;
        }
        Task bug = (Task) object;
        App.getLogic().setSelectedBug(bug);
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_bug");
        try {
            if (prev != null) {
                ft.remove(prev);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "performBugEdit removing dialog ", isex);
        }
        TaskFragment bugDialog = TaskFragment.newInstance(bug);
        try {
            bugDialog.show(fm, "fragment_bug");
        } catch (IllegalStateException isex) {
            // FIXME properly
            Log.e(DEBUG_TAG, "performBugEdit showing dialog ", isex);
        }
    }

    @Override
    public String getDescription(Object object) {
        if (!(object instanceof Task)) {
            Log.e(DEBUG_TAG, "Wrong object for " + getName() + " " + object.getClass().getName());
            return "?";
        }
        Task bug = (Task) object;
        return bug.getDescription(map.getContext());
    }
}
