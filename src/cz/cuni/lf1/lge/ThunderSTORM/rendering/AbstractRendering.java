package cz.cuni.lf1.lge.ThunderSTORM.rendering;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import static java.lang.Math.ceil;
import java.util.Arrays;

/**
 * A common abstract superclass implementing RenderingMethod and
 * IncrementalRenderingMethod. You can override the single abstract method
 * drawPoint to quickly add another Rendering Method.
 *
 * @author Josef Borkovec <josef.borkovec[at]lf1.cuni.cz>
 */
public abstract class AbstractRendering implements RenderingMethod, IncrementalRenderingMethod {

  protected double xmin, xmax, ymin, ymax;
  protected double resolution;
  protected int imSizeX, imSizeY;
  protected FloatProcessor image;
  protected double defaultDX;

  /**
   * A class for creating objects of sublasses of AbstractRendering.
   */
  protected static abstract class AbstractBuilder<BuilderType extends AbstractBuilder, BuiltType extends AbstractRendering> {

    protected double resolution, xmin = 0, xmax, ymin = 0, ymax;
    protected int imSizeY, imSizeX;
    protected boolean resolutionWasSet = false, roiWasSet = false, sizeWasSet = false;
    protected final static double defaultResolution = 20;
    private double defaultDX = 0.2;

    /**
     * Sets the desired resolution of the final image. In localization units per
     * pixel of the original image. <br> If the molecule locations are in nm,
     * value of 20 signifies that one pixel in the superresolution image will be
     * 20nm big <br> If the molecule locations are in pixels, a value of 0.2
     * signifies that the pixel size of the superresoultion image will be 0.2
     * times the pixel size of the original image.(5x smaller)
     *
     */
    public BuilderType resolution(double nmPerPixel) {
      if (nmPerPixel <= 0) {
        throw new IllegalArgumentException("Resolution must be positive. Passed value = " + nmPerPixel);
      }
      this.resolution = nmPerPixel;
      resolutionWasSet = true;
      return (BuilderType) this;
    }

    /**
     * Sets the region of interest. Only molecules inside the roi will be
     * rendered.
     *
     */
    public BuilderType roi(double xmin, double xmax, double ymin, double ymax) {
      if (xmax < xmin || ymax < ymin) {
        throw new IllegalArgumentException("xmax (ymax) must be greater than xmin (ymin)");
      }
      this.xmin = xmin;
      this.xmax = xmax;
      this.ymin = ymin;
      this.ymax = ymax;
      roiWasSet = true;
      return (BuilderType) this;
    }

    /**
     * Sets the size of the superresolution image
     */
    public BuilderType imageSize(int x, int y) {
      if (x <= 0 || y <= 0) {
        throw new IllegalArgumentException("Image size must be positive. Passed values = " + x + " " + y);
      }
      this.imSizeX = x;
      this.imSizeY = y;
      sizeWasSet = true;
      return (BuilderType) this;
    }

    public BuilderType defaultDX(double defaultDX) {
      if (defaultDX <= 0) {
        throw new IllegalArgumentException("Default dx must be positive. Passed value = " + defaultDX);
      }
      this.defaultDX = defaultDX;
      resolutionWasSet = true;
      return (BuilderType) this;
    }

    protected void validate() {
      if (!roiWasSet && !sizeWasSet) {
        throw new IllegalArgumentException("Image size must be resolved while building. Set at least image size or roi.");
      }
      if (!roiWasSet && sizeWasSet) {
        if (!resolutionWasSet) {
          resolution = defaultResolution;
        }
        xmax = imSizeX * resolution;
        ymax = imSizeY * resolution;
        xmin = 0;
        ymin = 0;
      } else if (roiWasSet && !sizeWasSet) {
        if (!resolutionWasSet) {
          resolution = defaultResolution;
        }
        imSizeX = (int) (ceil((xmax - xmin) / resolution));
        imSizeY = (int) (ceil((ymax - ymin) / resolution));
      } else {
        if (resolutionWasSet) {
          int newImSizeX = (int) ceil((xmax - xmin) / resolution);
          int newImSizeY = (int) ceil((ymax - ymin) / resolution);
          if (newImSizeX != imSizeX || newImSizeY != imSizeY) {
            throw new IllegalArgumentException("Invalid combination of image size, roi and resolution. Set only two of them.");
          }
        } else {
          resolution = xmax / imSizeX;
          if (Math.abs(resolution - ymax / imSizeY) > 0.0001) {
            throw new IllegalArgumentException("Resolution in x and y appears to be different.");
          }
        }
      }
    }

    /**
     * Returns the newly created object.
     */
    public abstract BuiltType build();
  }

  protected AbstractRendering(AbstractBuilder builder) {
    this.xmin = builder.xmin;
    this.xmax = builder.xmax;
    this.ymin = builder.ymin;
    this.ymax = builder.ymax;
    this.resolution = builder.resolution;
    this.imSizeX = builder.imSizeX;
    this.imSizeY = builder.imSizeY;
    this.defaultDX = builder.defaultDX;
    image = new FloatProcessor(imSizeX, imSizeY);
  }

  public void addToImage(double[] x, double[] y, double[] z, double[] dx) {
    for (int i = 0; i < x.length; i++) {
      double zVal = z != null ? z[i] : 0;
      double dxVal = dx != null ? dx[i] : defaultDX;
      drawPoint(x[i], y[i], zVal, dxVal);
    }
  }

  public ImagePlus getRenderedImage() {
    return new ImagePlus(this.getClass().getSimpleName(),image);
  }

  public ImagePlus getRenderedImage(double[] x, double[] y, double[] z, double[] dx) {
    reset();
    addToImage(x, y, z, dx);
    return getRenderedImage();
  }

  protected abstract void drawPoint(double x, double y, double z, double dx);

  public void reset() {
    float[] px = (float[]) image.getPixels();
    Arrays.fill(px, 0);
  }

  protected boolean isInBounds(double x, double y) {
    return x >= xmin && x < xmax && y >= ymin && y < ymax && !Double.isNaN(x) && !Double.isNaN(y);
  }
}