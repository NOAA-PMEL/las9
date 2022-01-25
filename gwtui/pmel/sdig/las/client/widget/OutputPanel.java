package pmel.sdig.las.client.widget;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import pmel.sdig.las.client.event.ImageLoaded;
import pmel.sdig.las.client.event.MapChangeEvent;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.main.UI;
import pmel.sdig.las.client.state.PanelState;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.MapScale;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 5/31/16.
 */
public class OutputPanel extends AbsolutePanel {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    List<UI.Mouse> mouseMoves = new ArrayList<UI.Mouse>();



    Canvas imageCanvas;
    Context2d imageCanvasContext;
    Canvas drawingCanvas;
    Context2d drawingCanvasContext;
    IESafeImage plotImage;
    ImageData scaledImage; // The scaled image data.

    // Drawing values
    double world_endx;
    double world_endy;
    double world_startx;
    double world_starty;

    int x_offset_from_left;
    int x_offset_from_right;

    double x_per_pixel;
    int x_plot_size;
    double y_axis_lower_left;
    double y_axis_upper_right;
    double x_axis_lower_left;
    double x_axis_upper_right;
    int y_image_size;
    int y_offset_from_bottom;
    int y_offset_from_top;
    double y_per_pixel;
    int y_plot_size;
    String time_origin;
    String time_units;
    String calendar;

    double dataMin;
    double dataMax;
    String levels;

    double imageScaleRatio = 1.0;

    int endx;
    int endy;
    int startx;
    int starty;

    // Take into account if the window is scrolled
    int yscroll;
    int xscroll;

    boolean outx = false;
    boolean outy = false;

    String axisVertical;
    String axisHorizontal;

    boolean draw = false;

    State state = new State();

    int scale_width = Constants.navWidth;

    public OutputPanel() {
        super();
        imageCanvas = Canvas.createIfSupported();
        drawingCanvas = Canvas.createIfSupported();
        add(imageCanvas, 0, 0);
        add(drawingCanvas, 0, 0);
        imageCanvasContext = imageCanvas.getContext2d();
        drawingCanvasContext = drawingCanvas.getContext2d();
        state.setPanelCount(1);
        // Don't make it fixed so the annotations will push it down.
//        addStyleName("output");

    }
    public void scale() {
        // Decide the image scale based on the available width
        scale(scale_width);
    }
    public void scale(int navWidth) {
        // Decide the image scale based on the available width
        scale_width = navWidth;
        int w = Window.getClientWidth() - (navWidth + Constants.imageBorderFactor) ;

        if (state.getPanelCount() == 1) {
            imageScaleRatio = 1.0d;
        } else if (state.getPanelCount() > 1) {
            w = w / 2;
        }


        if ( w < x_plot_size ) {
            double x = Double.valueOf(x_plot_size);
            double y = Double.valueOf(y_plot_size);
            double newx = Double.valueOf(w);
            double h = (y/x)*newx;
            imageScaleRatio = h/y_plot_size;
        }

        scale(plotImage, imageScaleRatio);
    }
    public void setStateForPanelCount(State state) {
        this.state = state;
        scale();
    }
    public void setState(State state) {

        this.state = state;

        PanelState myState = state.getPanelState(this.getTitle());

        // Set up from map scale.

        setFromMapScale(myState.getMapScale());

        clear();

        add(imageCanvas, 0, 0);
        add(drawingCanvas, 0, 0);


        plotImage = new IESafeImage(myState.getImageUrl());
        plotImage.addLoadHandler(imageLoadHandler);
        plotImage.setUrl(myState.getImageUrl());
        // In order for the image to load and for the onLoad to fire, the image must be loaded to the DOM.
        // The solution is to load it to the root, but invisible, then paint it to the canvas in the onLoad
        plotImage.setVisible(false);
        RootPanel.get().add(plotImage);
        drawingCanvas.addMouseUpHandler(new MouseUpHandler() {
            // Why do we add this later?
            @Override
            public void onMouseUp(MouseUpEvent event) {
                // If we're still drawing when the mouse
                // goes up, record the position.
                if (draw) {
                    endx = event.getX();
                    endy = event.getY();
                }
                draw = false;
                outx = false;
                outy = false;
                for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext(); ) {
                    UI.Mouse mouse = mouseIt.next();
                    mouse.applyNeeded();
                }
            }
        });
        scale();
    }
    LoadHandler imageLoadHandler = new LoadHandler() {

        @Override
        public void onLoad(LoadEvent event) {

            drawingCanvas.addMouseDownHandler(mouseDownHandler);
            drawingCanvas.addMouseUpHandler(mouseUpHandler);
            drawingCanvas.addMouseMoveHandler(mouseMoveHandler);
            eventBus.fireEventFromSource(new ImageLoaded(), OutputPanel.this);

            scale();

        }
    };

    MouseDownHandler mouseDownHandler = new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
            outx = false;
            outy = false;
            startx = event.getX();
            starty = event.getY();

            yscroll = Document.get().getScrollTop();
            xscroll = Document.get().getScrollLeft();
            starty = starty - yscroll;
            startx = startx - xscroll;

            // TODO DEBUG promise to only click in the image
            draw = true;

            if (startx > x_offset_from_left && starty > y_offset_from_top && startx < x_offset_from_left + x_plot_size && starty < y_offset_from_top + y_plot_size) {

                draw = true;
                drawToScreen(scaledImage); // frontCanvasContext.drawImage(ImageElement.as(plotImage.getElement()),
                // 0, 0);
                double scaled_x_per_pixel = x_per_pixel / imageScaleRatio;
                double scaled_y_per_pixel = y_per_pixel / imageScaleRatio;
                world_startx = x_axis_lower_left + (startx - x_offset_from_left * imageScaleRatio) * scaled_x_per_pixel;
                world_starty = y_axis_lower_left + ((y_image_size * imageScaleRatio - starty) - y_offset_from_bottom * imageScaleRatio) * scaled_y_per_pixel;

                world_endx = world_startx;
                world_endy = world_starty;
            }

        }
    };

    public void clearPlot() {
        int w = imageCanvas.getCoordinateSpaceWidth();
        int h = imageCanvas.getCoordinateSpaceHeight();
        scaledImage = null;
        imageCanvasContext.clearRect(0, 0, w, h);
    }
    public void clearOverlay() {
        drawingCanvasContext.clearRect(0, 0, drawingCanvas.getCoordinateSpaceWidth(), drawingCanvas.getCoordinateSpaceHeight());
    }
    public void setFromMapScale(MapScale mapScale) {

        x_offset_from_left = Integer.valueOf(mapScale.getXxxOffsetFromLeft());
        y_offset_from_top = Integer.valueOf(mapScale.getYyyOffsetFromTop());

        x_offset_from_right = Integer.valueOf(mapScale.getXxxOffsetFromRight());
        y_offset_from_bottom = Integer.valueOf(mapScale.getYyyOffsetFromBottom());

        x_plot_size = Integer.valueOf(mapScale.getXxxPlotSize());
        y_plot_size = Integer.valueOf(mapScale.getYyyPlotSize());

        y_axis_lower_left = Double.valueOf(mapScale.getYyyAxisLowerLeft());
        x_axis_lower_left = Double.valueOf(mapScale.getXxxAxisLowerLeft());

        x_axis_upper_right = Double.valueOf(mapScale.getXxxAxisUpperRight());
        y_axis_upper_right = Double.valueOf(mapScale.getYyyAxisUpperRight());

        x_per_pixel = (x_axis_upper_right - x_axis_lower_left) / Double.valueOf(x_plot_size);
        y_per_pixel = (y_axis_upper_right - y_axis_lower_left) / Double.valueOf(y_plot_size);

        y_image_size = Integer.valueOf(mapScale.getYyyImageSize());

        axisVertical = mapScale.getAxis_vertical();
        axisHorizontal = mapScale.getAxis_horizontal();

        dataMin = Double.valueOf(mapScale.getData_max());
        dataMax = Double.valueOf(mapScale.getData_max());

        levels = mapScale.getLevels_string();

        time_origin = mapScale.getTime_origin();
        time_units = mapScale.getTime_units();
        calendar = mapScale.getCalendar();

    }

    MouseMoveHandler mouseMoveHandler = new MouseMoveHandler() {

        @Override
        public void onMouseMove(MouseMoveEvent event) {

            int currentx = event.getX();
            int currenty = event.getY();
            yscroll = Document.get().getScrollTop();
            xscroll = Document.get().getScrollLeft();
            currenty = currenty - yscroll;
            currentx = currentx - xscroll;
            // If you drag it out, we'll stop drawing.

            if (currentx < x_offset_from_left* imageScaleRatio || currenty < y_offset_from_top * imageScaleRatio ||
                    currentx > x_offset_from_left * imageScaleRatio + x_plot_size * imageScaleRatio ||
                    currenty > y_offset_from_top * imageScaleRatio + y_plot_size * imageScaleRatio) {

                endx = currentx;
                endy = currenty;

                // Set the limits for one last drawing of the selection
                // rectangle.
                if (currentx < x_offset_from_left * imageScaleRatio ) {
                    endx = (int) (x_offset_from_left * imageScaleRatio);
                    currentx = (int) (x_offset_from_left * imageScaleRatio);
                    outx = true;
                }
                if (currenty < y_offset_from_top * imageScaleRatio) {
                    endy = (int) (y_offset_from_top * imageScaleRatio);
                    currenty = (int) (y_offset_from_top* imageScaleRatio);
                    outy = true;
                }
                if (currentx > x_offset_from_left * imageScaleRatio + x_plot_size * imageScaleRatio) {
                    endx = (int) (x_offset_from_left * imageScaleRatio + x_plot_size * imageScaleRatio);
                    currentx = (int) (x_offset_from_left * imageScaleRatio + x_plot_size * imageScaleRatio);
                    outx = true;
                }
                if (currenty > y_offset_from_top * imageScaleRatio + y_plot_size * imageScaleRatio) {
                    endy = (int) (y_offset_from_top * imageScaleRatio + y_plot_size * imageScaleRatio);
                    currenty = (int) (y_offset_from_top * imageScaleRatio + y_plot_size * imageScaleRatio);
                    outy = true;
                }
            }
            if (draw) {
                double scaled_x_per_pixel = x_per_pixel / imageScaleRatio;
                double scaled_y_per_pixel = y_per_pixel / imageScaleRatio;
                world_endx = x_axis_lower_left + (currentx - x_offset_from_left * imageScaleRatio) * scaled_x_per_pixel;
                world_endy = y_axis_lower_left + ((y_image_size * imageScaleRatio - currenty) - y_offset_from_bottom * imageScaleRatio) * scaled_y_per_pixel;
                drawingCanvasContext.setFillStyle(Constants.randomColor);
                drawingCanvasContext.clearRect(0, 0, drawingCanvas.getCoordinateSpaceWidth(), drawingCanvas.getCoordinateSpaceHeight());
                drawingCanvasContext.fillRect(startx, starty, currentx - startx, currenty - starty);
                drawingCanvasContext.strokeRect(startx, starty, currentx - startx, currenty - starty);
                //for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext();) {
                for (int m = 0; m < mouseMoves.size(); m++ ) {
                    //UI.Mouse mouse = mouseIt.next();
                    UI.Mouse mouse = mouseMoves.get(m);
                    double minx = Math.min(world_startx, world_endx);
                    double maxx = Math.max(world_startx, world_endx);
                    double miny = Math.min(world_starty, world_endy);
                    double maxy = Math.max(world_starty, world_endy);
                    if (axisVertical.equals("y") && axisHorizontal.equals("x")) {
                        mouse.updateMap(miny, maxy, minx, maxx);
                    } else if (axisVertical.equals("x") && axisHorizontal.equals("y")) {
                        mouse.updateMap(minx, maxx, miny, maxy);
                    } else if (axisVertical.equals("y") && !axisHorizontal.equals("x")) {
                        mouse.updateLat(miny, maxy);
                    } else if (axisVertical.equals("x") && !axisHorizontal.equals("y")) {
                        mouse.updateLon(miny, maxy);
                    } else if (!axisVertical.equals("y") && axisHorizontal.equals("x")) {
                        mouse.updateLon(minx, maxx);
                    } else if (!axisVertical.equals("x") && axisHorizontal.equals("y")) {
                        mouse.updateLat(minx, maxx);
                    } else {
                        mouse.updateConstraints(world_startx, world_endx, world_starty, world_endy);
                    }
                }
            }
            if (outx && outy) {
                draw = false;
            }
        }
    };
    MouseUpHandler mouseUpHandler = new MouseUpHandler() {

        @Override
        public void onMouseUp(MouseUpEvent arg0) {
            double minx = Math.min(world_startx, world_endx);
            double maxx = Math.max(world_startx, world_endx);
            double miny = Math.min(world_starty, world_endy);
            double maxy = Math.max(world_starty, world_endy);

            // || (axisVertical.equals("d") && !operationID.equals("Timeseries_station_plot"))
            if (axisVertical.equals("z")  ) {
                for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext();) {
                    UI.Mouse mouse = mouseIt.next();
                    mouse.setZ(miny, maxy);
                }
            }
            // || (axisHorizontal.equals("d") && !operationID.equals("Timeseries_station_plot"))
            if (axisHorizontal.equals("z")  ) {
                for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext();) {
                    UI.Mouse mouse = mouseIt.next();
                    mouse.setZ(minx, maxx);
                }
            }

            if ( axisVertical.equals("t") ) {
                for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext();) {
                    UI.Mouse mouse = mouseIt.next();
                    mouse.updateTime(miny, maxy, time_origin, time_units, calendar);
                }
            }
                                               // Time in a set of profiles
            if ( axisHorizontal.equals("t") || axisHorizontal.equals("e")) {
                for (Iterator<UI.Mouse> mouseIt = mouseMoves.iterator(); mouseIt.hasNext();) {
                    UI.Mouse mouse = mouseIt.next();
                    mouse.updateTime(minx, maxx, time_origin, time_units, calendar);
                }
            }

            eventBus.fireEvent(new MapChangeEvent(miny, maxy, minx, maxx));


        }
    };


    private void drawToScreen(ImageData imageData) {
        // Don't bother to use null imageData

        if ((imageData != null) && (imageCanvasContext != null)) {
            imageCanvasContext.putImageData(imageData, 0, 0);
        }
    }


    private void scale(Image img, double scaleRatio) {
        scaledImage = scaleImage(img, scaleRatio);
        drawToScreen(scaledImage);
    }

    private ImageData scaleImage(Image image, double scaleToRatio) {
        Canvas canvasTmp = Canvas.createIfSupported();
        Context2d context = canvasTmp.getContext2d();
        ImageData imageData = null;
        if ( image != null ) {
            int imageHeight = image.getHeight();

            double ch = (imageHeight * scaleToRatio);
            int imageWidth = image.getWidth();

            double cw = (imageWidth * scaleToRatio);

            canvasTmp.setCoordinateSpaceHeight((int) ch);
            canvasTmp.setCoordinateSpaceWidth((int) cw);

            // TODO: make a temp imageElement?
            ImageElement imageElement = ImageElement.as(image.getElement());

            // s = source
            // d = destination
            double sx = 0;
            double sy = 0;
            int imageElementWidth = imageElement.getWidth();
            if (imageElementWidth <= 0) {
                imageElementWidth = imageWidth;
            }
            double sw = imageElementWidth;
            int imageElementHeight = imageElement.getHeight();
            if (imageElementHeight <= 0) {
                imageElementHeight = imageHeight;
            }
            double sh = imageElementHeight;

            double dx = 0;
            double dy = 0;
            double dw = imageElementWidth;
            double dh = imageElementHeight;

            // tell it to scale image
            context.scale(scaleToRatio, scaleToRatio);

            // draw image to canvas
            context.drawImage(imageElement, sx, sy, sw, sh, dx, dy, dw, dh);

            // get image data
            double w = dw * scaleToRatio;
            double h = dh * scaleToRatio;

            try {
                imageData = context.getImageData(0, 0, w, h);
            } catch (Exception e) {
                // no image data. we'll try again...
                String b = e.getLocalizedMessage();
            }

            int ht = (int) h + 10;
            int wt = (int) w + 10;

            // Clear the div, clear the drawing canvas then reinsert.  Otherwise, ghosts of the previous image appear.
            clear();

            imageCanvasContext.clearRect(0, 0, imageCanvas.getCoordinateSpaceWidth(), imageCanvas.getCoordinateSpaceHeight());

            add(imageCanvas, 0, 0);
            add(drawingCanvas, 0, 0);

            imageCanvas.setCoordinateSpaceHeight(ht);
            imageCanvas.setCoordinateSpaceWidth(wt);

            drawingCanvas.setCoordinateSpaceHeight(ht);
            drawingCanvas.setCoordinateSpaceWidth(wt);

            setSize(wt + "px", ht + "px");
        }
            return imageData;

    }
    public void addMouse(UI.Mouse m) {
        mouseMoves.add(m);
    }

    public State getState() {
        return state;
    }

    public double getDataMin() {
        return dataMin;
    }

    public double getDataMax() {
        return dataMax;
    }

    public String getLevels() {
        return levels;
    }
    public IESafeImage getPlotImage() {
        return plotImage;
    }
}
