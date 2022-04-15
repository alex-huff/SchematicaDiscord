package phonis.SchematicaDownload.util;

public class ImageColor
{

    private double r;
    private double g;
    private double b;

    public ImageColor(double r, double g, double b)
    {
        this.setR(r);
        this.setG(g);
        this.setB(b);
    }

    public double getR()
    {
        return this.r;
    }

    private void setR(double r)
    {
        if (r > 255)
        {
            this.r = 255;
        }
        else if (r < 0)
        {
            this.r = 0;
        }
        else
        {
            this.r = r;
        }
    }

    public double getG()
    {
        return this.g;
    }

    private void setG(double g)
    {
        if (g > 255)
        {
            this.g = 255;
        }
        else if (g < 0)
        {
            this.g = 0;
        }
        else
        {
            this.g = g;
        }
    }

    public double getB()
    {
        return this.b;
    }

    private void setB(double b)
    {
        if (b > 255)
        {
            this.b = 255;
        }
        else if (b < 0)
        {
            this.b = 0;
        }
        else
        {
            this.b = b;
        }
    }

    public void addR(double r)
    {
        this.setR(this.r + r);
    }

    public void addG(double g)
    {
        this.setG(this.g + g);
    }

    public void addB(double b)
    {
        this.setB(this.b + b);
    }

}
