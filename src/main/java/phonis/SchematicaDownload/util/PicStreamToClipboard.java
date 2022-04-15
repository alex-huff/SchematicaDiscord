package phonis.SchematicaDownload.util;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class PicStreamToClipboard implements ClipboardConverter
{

    private final String args;

    public PicStreamToClipboard(String args)
    {
        this.args = args;
    }

    @Override
    public Clipboard getClipboard(InputStream stream) throws ClipboardException
    {
        BufferedImage pic;

        try
        {
            pic = ImageIO.read(stream);

            stream.close();
        }
        catch (IOException e)
        {
            throw new ClipboardException("IOException reading image.");
        }

        String[] dimensions = this.args.split(" ");
        int[][]  palette;
        int      width, height;
        boolean  dither     = true;

        if (dimensions.length >= 2)
        {
            try
            {
                width  = Integer.parseInt(dimensions[0]);
                height = Integer.parseInt(dimensions[1]);

                if (width < 1 || height < 1)
                {
                    throw new ClipboardException("Cannot have zero or negative dimension.");
                }
            }
            catch (NumberFormatException e)
            {
                throw new ClipboardException("Invalid dimensions.");
            }

            if (dimensions.length >= 3)
            {
                if (dimensions[2].equalsIgnoreCase("wool"))
                {
                    palette = DitherBlockPalette.paletteWoolOnly;
                }
                else if (dimensions[2].equalsIgnoreCase("concrete"))
                {
                    palette = DitherBlockPalette.paletteConcreteOnly;
                }
                else if (dimensions[2].equalsIgnoreCase("terracotta"))
                {
                    palette = DitherBlockPalette.paletteTerracottaOnly;
                }
                else if (dimensions[2].equalsIgnoreCase("woolandconcrete"))
                {
                    palette = DitherBlockPalette.paletteWoolAndConcrete;
                }
                else
                {
                    throw new ClipboardException("Invalid palette.");
                }
            }
            else
            {
                palette = DitherBlockPalette.palette;
            }

            if (dimensions.length >= 4)
            {
                if (dimensions[3].equalsIgnoreCase("closest"))
                {
                    dither = false;
                }
                else if (dimensions[3].equalsIgnoreCase("dither"))
                {
                }
                else
                {
                    throw new ClipboardException("Invalid algorithm.");
                }
            }
        }
        else
        {
            width   = pic.getWidth();
            height  = pic.getHeight();
            palette = DitherBlockPalette.palette;
        }

        BufferedImage resized = new BufferedImage(width, height, pic.getType());
        Graphics2D    gr      = resized.createGraphics();

        gr.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        gr.drawImage(pic, 0, 0, width, height, 0, 0, pic.getWidth(),
                     pic.getHeight(), null
        );
        gr.dispose();

        return this.clipboardFromImage(resized, palette, dither);
    }

    private Clipboard clipboardFromImage(BufferedImage resized, int[][] palette, boolean dither)
        throws ClipboardException
    {
        int        width  = resized.getWidth();
        int        height = resized.getHeight();
        int[][]    image  = dither ? this.getDitheredImage(resized, palette) : this.getImage(resized, palette);
        Material[] values = Material.values();
        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(0, 0, 0), BlockVector3.at(width - 1, 0, height - 1));
        BlockArrayClipboard bac = new BlockArrayClipboard(region);

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                try
                {
                    bac.setBlock(
                        BlockVector3.at(
                            x,
                            0,
                            y
                        ),
                        FuzzyBlockState.builder().type(
                            Objects.requireNonNull(
                                BukkitAdapter.asBlockType(
                                    values[image[x][y] == -1 ? Material.AIR.ordinal() : palette[image[x][y]][0]]
                                )
                            )
                        ).build()
                    );
                }
                catch (WorldEditException e)
                {
                    throw new ClipboardException("WorldEdit exception.");
                }
            }
        }

        return bac;
    }

    private ImageColor RGBAtoRGB(int r, int g, int b, int a)
    {
        // black background
        int    br    = 0;
        int    bg    = 0;
        int    bb    = 0;
        double alpha = a / 255d;

        if (alpha <= .8d) return null;

        return new ImageColor(
            (1 - alpha) * br + alpha * r,
            (1 - alpha) * bg + alpha * g,
            (1 - alpha) * bb + alpha * b
        );
    }

    private int[][] getImage(BufferedImage resized, int[][] palette)
    {
        int            width      = resized.getWidth();
        int            height     = resized.getHeight();
        ImageColor[][] imageArray = new ImageColor[width][height];
        int[][]        palettized = new int[width][height];
        boolean        hasAlpha   = resized.getColorModel().hasAlpha();

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                int p = resized.getRGB(x, y);
                int a = hasAlpha ? (p >> 24) & 0xff : 255;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                imageArray[x][y] = !hasAlpha ? new ImageColor(r, g, b) : this.RGBAtoRGB(r, g, b, a);
            }
        }

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                ImageColor current = imageArray[x][y];

                if (current == null)
                {
                    palettized[x][y] = -1;

                    continue;
                }

                int best = this.getClosest(current, palette);
                palettized[x][y] = best;
            }
        }

        return palettized;
    }

    private int[][] getDitheredImage(BufferedImage resized, int[][] palette)
    {
        int            width      = resized.getWidth();
        int            height     = resized.getHeight();
        ImageColor[][] imageArray = new ImageColor[width][height];
        int[][]        dithered   = new int[width][height];
        boolean        hasAlpha   = resized.getColorModel().hasAlpha();

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                int p = resized.getRGB(x, y);
                int a = hasAlpha ? (p >> 24) & 0xff : 255;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                imageArray[x][y] = !hasAlpha ? new ImageColor(r, g, b) : this.RGBAtoRGB(r, g, b, a);
            }
        }

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                ImageColor current = imageArray[x][y];

                if (current == null)
                {
                    dithered[x][y] = -1;

                    continue;
                }

                int best = this.getClosest(current, palette);
                dithered[x][y] = best;
                ImageColor bestColor = new ImageColor(
                    palette[best][1],
                    palette[best][2],
                    palette[best][3]
                );
                double[] error = this.getDifference(current, bestColor);

                if (x + 1 < width && imageArray[x + 1][y] != null)
                {
                    imageArray[x + 1][y].addR(error[0] * 7.0 / 16);
                    imageArray[x + 1][y].addG(error[1] * 7.0 / 16);
                    imageArray[x + 1][y].addB(error[2] * 7.0 / 16);
                }

                if (y + 1 < height)
                {
                    if (x - 1 > 0 && imageArray[x - 1][y + 1] != null)
                    {
                        imageArray[x - 1][y + 1].addR(error[0] * 3.0 / 16);
                        imageArray[x - 1][y + 1].addG(error[1] * 3.0 / 16);
                        imageArray[x - 1][y + 1].addB(error[2] * 3.0 / 16);
                    }

                    if (imageArray[x][y + 1] != null)
                    {
                        imageArray[x][y + 1].addR(error[0] * 5.0 / 16);
                        imageArray[x][y + 1].addG(error[1] * 5.0 / 16);
                        imageArray[x][y + 1].addB(error[2] * 5.0 / 16);
                    }

                    if (x + 1 < width && imageArray[x + 1][y + 1] != null)
                    {
                        imageArray[x + 1][y + 1].addR(error[0] * 1.0 / 16);
                        imageArray[x + 1][y + 1].addG(error[1] * 1.0 / 16);
                        imageArray[x + 1][y + 1].addB(error[2] * 1.0 / 16);
                    }
                }
            }
        }

        return dithered;
    }

    private int getClosest(ImageColor color, int[][] palette)
    {
        double closest = Double.MAX_VALUE;
        int    best    = 0;

        for (int i = 0; i < palette.length; i++)
        {
            double heuristic =
                Math.pow(color.getR() - palette[i][1], 2) +
                Math.pow(color.getG() - palette[i][2], 2) +
                Math.pow(color.getB() - palette[i][3], 2);

            if (heuristic < closest)
            {
                closest = heuristic;
                best    = i;
            }
        }

        return best;
    }

    private double[] getDifference(ImageColor one, ImageColor two)
    {
        return new double[]{
            one.getR() - two.getR(),
            one.getG() - two.getG(),
            one.getB() - two.getB()
        };
    }

}


