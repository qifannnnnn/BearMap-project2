package bearmaps.proj2c.server.handler.impl;

import bearmaps.proj2c.AugmentedStreetMapGraph;
import bearmaps.proj2c.server.handler.APIRouteHandler;
import spark.Request;
import spark.Response;
import bearmaps.proj2c.utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static bearmaps.proj2c.utils.Constants.*;

/**
 * Handles requests from the web browser for map images. These images
 * will be rastered into one large image to be displayed to the user.
 * @author rahul, Josh Hug, _________
 */
public class RasterAPIHandler extends APIRouteHandler<Map<String, Double>, Map<String, Object>> {

    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside RasterAPIHandler.processRequest(). <br>
     * ullat : upper left corner latitude, <br> ullon : upper left corner longitude, <br>
     * lrlat : lower right corner latitude,<br> lrlon : lower right corner longitude <br>
     * w : user viewport window width in pixels,<br> h : user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
            "lrlon", "w", "h"};

    /**
     * The result of rastering must be a map containing all of the
     * fields listed in the comments for RasterAPIHandler.processRequest.
     **/
    private static final String[] REQUIRED_RASTER_RESULT_PARAMS = {"render_grid", "raster_ul_lon",
            "raster_ul_lat", "raster_lr_lon", "raster_lr_lat", "depth", "query_success"};


    @Override
    protected Map<String, Double> parseRequestParams(Request request) {
        return getRequestParams(request, REQUIRED_RASTER_REQUEST_PARAMS);
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param requestParams Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @param response : Not used by this function. You may ignore.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image;
     *                    can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
   /* @Override
    public Map<String, Object> processRequest(Map<String, Double> requestParams, Response response){
        Map<String, Object> results = new HashMap<>();
        return results;
    }*/
    @Override
    public Map<String, Object> processRequest(Map<String, Double> requestParams, Response response) {
        System.out.println("yo, wanna know the parameters given by the web browser? They are:");
        System.out.println(requestParams);
        Map<String, Object> results = new HashMap<>();
        // Calculate the proper depth - first dimension of the image
        int depth;
        depth=getDepth(requestParams);

        int numOfGrid=(int) Math.pow(2,depth);
        double interval_long=(ROOT_LRLON-ROOT_ULLON)/numOfGrid;
        double interval_lat=(ROOT_ULLAT-ROOT_LRLAT)/numOfGrid;

        results.put("query_success",true);

        // case 1 out of the available area
        if (requestParams.get("ullon")>requestParams.get("lrlon")){
             results.put("query_success",false);
        }

        //
        else if (outOfBox(requestParams)){
            results.put("query_success",false);
        }

        // non-edge cases
        int UL_x=getULX(requestParams,interval_long);
        int UL_y=getULY(requestParams, interval_lat);
        int LR_x=getLRX(requestParams,interval_long);
        int LR_y=getLRY(requestParams, interval_lat);

        results.put("depth",depth);
        results.put("raster_ul_lon", ROOT_ULLON+(UL_x)*interval_long);
        results.put("raster_ul_lat", ROOT_ULLAT-(UL_y)*interval_lat);
        results.put("raster_lr_lon", ROOT_ULLON+(LR_x+1)*interval_long);
        results.put("raster_lr_lat", ROOT_ULLAT-(LR_y+1)*interval_lat);

        String[][] grid=new String[LR_y-UL_y+1][LR_x-UL_x+1];

        for (int i=0;i<LR_y-UL_y+1;i++){
            for (int j=0;j<LR_x-UL_x+1;j++){
                int num1=UL_x+j;
                int num2=UL_y+i;
                grid[i][j]="d"+depth+"_"+"x"+ num1+"_"+"y"+num2+".png";
            }
        }

        results.put("render_grid",grid);
       // System.out.println(LR_x-UL_x);
        return results;
    }

    private boolean outOfBox (Map<String, Double> requestParams){

        if (requestParams.get("ullon")>ROOT_LRLON){
            return true;
        }

        if (requestParams.get("ullat")<ROOT_LRLAT){
            return true;
        }

        if (requestParams.get("lrlon")<ROOT_ULLON){
            return true;
        }

        if (requestParams.get("lrlat")>ROOT_ULLAT){
            return true;
        }

        return false;
    }

    private int getLRX(Map<String, Double> requestParams, double interval){
        int LRX=0;
        if (requestParams.get("lrlon")>ROOT_LRLON){
            LRX=(int) Math.floor((ROOT_LRLON-ROOT_ULLON)/interval)-1;;
        }
        else{
            LRX=(int) Math.floor((requestParams.get("lrlon")-ROOT_ULLON)/interval);
        }
        return LRX;
    }

    private int getLRY(Map<String, Double> requestParams, double interval){
        int LRY=0;
        if (requestParams.get("lrlat")<ROOT_LRLAT){
            LRY=(int) Math.floor((ROOT_ULLAT-ROOT_LRLAT)/interval)-1;
        }
        else{
        LRY=(int) Math.floor((ROOT_ULLAT-requestParams.get("lrlat"))/interval);}
        return LRY;
    }

    private int getULX(Map<String, Double> requestParams, double interval){
        int ULX=0;
        if (requestParams.get("ullon")<ROOT_ULLON){
            ULX=0;
        }
        else {
            ULX = (int) Math.floor((requestParams.get("ullon") - ROOT_ULLON) / interval);
        }
        return ULX;
    }

    private int getULY(Map<String, Double> requestParams, double interval){
        int ULY=0;
        if (requestParams.get("ullat")>ROOT_ULLAT){
            ULY=0;
        }
        else {
            ULY = (int) Math.floor((ROOT_ULLAT-requestParams.get("ullat")) / interval);
        }
        return ULY;
    }



    private int getDepth(Map<String, Double> requestParams){
        int depth=0;
        // desired longitude per pixel
        double desired=(requestParams.get("lrlon")-requestParams.get("ullon"))/requestParams.get("w");
        double largest=(ROOT_LRLON-ROOT_ULLON)/TILE_SIZE;
        double smallest=largest/Math.pow(2,7);

        // when the desired longitude/pixel is larger than the largest
        if (desired>largest){
            depth=0;}

        // when the desired is smaller than the smallest
        else if (desired<smallest){
            depth=7;
        }

        else{

            double mul=largest/desired;
            depth= (int) Math.ceil(Math.log10(mul)/Math.log10(2));
        }

        return depth;
    }


    @Override
    protected Object buildJsonResponse(Map<String, Object> result) {
        boolean rasterSuccess = validateRasteredImgParams(result);

        if (rasterSuccess) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeImagesToOutputStream(result, os);
            String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
            result.put("b64_encoded_image_data", encodedImage);
        }
        return super.buildJsonResponse(result);
    }

    private Map<String, Object> queryFail() {
        Map<String, Object> results = new HashMap<>();
        results.put("render_grid", null);
        results.put("raster_ul_lon", 0);
        results.put("raster_ul_lat", 0);
        results.put("raster_lr_lon", 0);
        results.put("raster_lr_lat", 0);
        results.put("depth", 0);
        results.put("query_success", false);
        return results;
    }

    /**
     * Validates that Rasterer has returned a result that can be rendered.
     * @param rip : Parameters provided by the rasterer
     */
    private boolean validateRasteredImgParams(Map<String, Object> rip) {
        for (String p : REQUIRED_RASTER_RESULT_PARAMS) {
            if (!rip.containsKey(p)) {
                System.out.println("Your rastering result is missing the " + p + " field.");
                return false;
            }
        }
        if (rip.containsKey("query_success")) {
            boolean success = (boolean) rip.get("query_success");
            if (!success) {
                System.out.println("query_success was reported as a failure");
                return false;
            }
        }
        return true;
    }

    /**
     * Writes the images corresponding to rasteredImgParams to the output stream.
     * In Spring 2016, students had to do this on their own, but in 2017,
     * we made this into provided code since it was just a bit too low level.
     */
    private  void writeImagesToOutputStream(Map<String, Object> rasteredImageParams,
                                                  ByteArrayOutputStream os) {
        String[][] renderGrid = (String[][]) rasteredImageParams.get("render_grid");
        int numVertTiles = renderGrid.length;
        int numHorizTiles = renderGrid[0].length;

        BufferedImage img = new BufferedImage(numHorizTiles * Constants.TILE_SIZE,
                numVertTiles * Constants.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics graphic = img.getGraphics();
        int x = 0, y = 0;

        for (int r = 0; r < numVertTiles; r += 1) {
            for (int c = 0; c < numHorizTiles; c += 1) {
                graphic.drawImage(getImage(Constants.IMG_ROOT + renderGrid[r][c]), x, y, null);
                x += Constants.TILE_SIZE;
                if (x >= img.getWidth()) {
                    x = 0;
                    y += Constants.TILE_SIZE;
                }
            }
        }

        /* If there is a route, draw it. */
        double ullon = (double) rasteredImageParams.get("raster_ul_lon"); //tiles.get(0).ulp;
        double ullat = (double) rasteredImageParams.get("raster_ul_lat"); //tiles.get(0).ulp;
        double lrlon = (double) rasteredImageParams.get("raster_lr_lon"); //tiles.get(0).ulp;
        double lrlat = (double) rasteredImageParams.get("raster_lr_lat"); //tiles.get(0).ulp;

        final double wdpp = (lrlon - ullon) / img.getWidth();
        final double hdpp = (ullat - lrlat) / img.getHeight();
        AugmentedStreetMapGraph graph = SEMANTIC_STREET_GRAPH;
        List<Long> route = ROUTE_LIST;

        if (route != null && !route.isEmpty()) {
            Graphics2D g2d = (Graphics2D) graphic;
            g2d.setColor(Constants.ROUTE_STROKE_COLOR);
            g2d.setStroke(new BasicStroke(Constants.ROUTE_STROKE_WIDTH_PX,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            route.stream().reduce((v, w) -> {
                g2d.drawLine((int) ((graph.lon(v) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(v)) * (1 / hdpp)),
                        (int) ((graph.lon(w) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(w)) * (1 / hdpp)));
                return w;
            });
        }

        rasteredImageParams.put("raster_width", img.getWidth());
        rasteredImageParams.put("raster_height", img.getHeight());

        try {
            ImageIO.write(img, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private BufferedImage getImage(String imgPath) {
        BufferedImage tileImg = null;
        if (tileImg == null) {
            try {
                File in = new File(imgPath);
                tileImg = ImageIO.read(in);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return tileImg;
    }

    public static void main(String[] Args){
        RasterAPIHandler handler=new RasterAPIHandler();
        Map<String, Double> Params=new HashMap<>();
        Params.put("lrlon",-122.24053369025242);
        Params.put("ullon",-122.24163047377972);
        Params.put("w",892.0);
        Params.put("h",875.0);
        Params.put("ullat",37.87655856892288);
        Params.put("lrlat",37.87548268822065);
        //{lrlon=-122.24053369025242, ullon=-122.24163047377972,
        // w=892.0, h=875.0, ullat=37.87655856892288, lrlat=37.87548268822065}
      //  Map<String, Object> r=handler.processRequest(Params);
        //System.out.print(r);
        //System.out.println(Arrays.deepToString((Object[]) r.get("render_grid")));
    }

}
