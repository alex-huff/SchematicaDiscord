package phonis.SchematicaDownload.util;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class PicStreamToClipboard implements ClipboardConverter {

    private String args;

    public PicStreamToClipboard(String args) {
        this.args = args;
    }

    @Override
    public Clipboard getClipboard(InputStream stream) throws ClipboardException {
        BufferedImage pic;

        try {
            pic = ImageIO.read(stream);

            stream.close();
        } catch (IOException e) {
            throw new ClipboardException("IOException reading image.");
        }

        String[] dimensions = this.args.split(" ");
        int[][] palette;
        int width, height;

        if (dimensions.length >= 2) {
            try {
                width = Integer.parseInt(dimensions[0]);
                height = Integer.parseInt(dimensions[1]);

                if (width < 1 || height < 1) {
                    throw new ClipboardException("Cannot have zero or negative dimension.");
                }
            } catch (NumberFormatException e) {
                throw new ClipboardException("Invalid dimensions.");
            }

            if (dimensions.length == 3) {
                if (dimensions[2].equalsIgnoreCase("wool")) {
                    palette = DitherBlockPalette.paletteWoolOnly;
                } else if (dimensions[2].equalsIgnoreCase("clay")) {
                    palette = DitherBlockPalette.paletteClayOnly;
                } else {
                    throw new ClipboardException("Invalid palette.");
                }
            } else {
                palette = DitherBlockPalette.palette;
            }
        } else {
            width = pic.getWidth();
            height = pic.getHeight();
            palette = DitherBlockPalette.palette;
        }

        BufferedImage resized = new BufferedImage(width, height, pic.getType());
        Graphics2D gr = resized.createGraphics();

        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gr.drawImage(pic, 0, 0, width, height, 0, 0, pic.getWidth(),
            pic.getHeight(), null);
        gr.dispose();

        return this.clipboardFromImage(resized, palette);
    }

    private Clipboard clipboardFromImage(BufferedImage resized, int[][] palette) throws ClipboardException {
        int width = resized.getWidth();
        int height = resized.getHeight();
        int[][] dithered = this.getDitheredImage(resized, palette);
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(width - 1, 0, height - 1));
        BlockArrayClipboard bac = new BlockArrayClipboard(region);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int[] pal = palette[dithered[x][y]];
                try {
                    bac.setBlock(
                        new Vector(
                            x,
                            0,
                            y
                        ),
                        new BaseBlock(pal[0], pal[1])
                    );
                } catch (WorldEditException e) {
                    throw new ClipboardException("WorldEdit exception.");
                }
            }
        }

        return bac;
    }

    private int[][] getDitheredImage(BufferedImage resized, int[][] palette) {
        int width = resized.getWidth();
        int height = resized.getHeight();
        DitherColor[][] imageArray = new DitherColor[width][height];
        int[][] dithered = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int p = resized.getRGB(x, y);
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                imageArray[x][y] = new DitherColor(r, g, b);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                DitherColor current = imageArray[x][y];
                int best = this.getClosest(current, palette);
                dithered[x][y] = best;
                DitherColor bestColor = new DitherColor(
                    palette[best][2],
                    palette[best][3],
                    palette[best][4]
                );
                double[] error = this.getDifference(current, bestColor);

                if (x + 1 < width) {
                    imageArray[x + 1][y].addR(error[0] * 7.0 / 16);
                    imageArray[x + 1][y].addG(error[1] * 7.0 / 16);
                    imageArray[x + 1][y].addB(error[2] * 7.0 / 16);
                }

                if (y + 1 < height) {
                    if (x - 1 > 0) {
                        imageArray[x - 1][y + 1].addR(error[0] * 3.0 / 16);
                        imageArray[x - 1][y + 1].addG(error[1] * 3.0 / 16);
                        imageArray[x - 1][y + 1].addB(error[2] * 3.0 / 16);
                    }

                    imageArray[x][y + 1].addR(error[0] * 5.0 / 16);
                    imageArray[x][y + 1].addG(error[1] * 5.0 / 16);
                    imageArray[x][y + 1].addB(error[2] * 5.0 / 16);

                    if (x + 1 < width) {
                        imageArray[x + 1][y + 1].addR(error[0] * 1.0 / 16);
                        imageArray[x + 1][y + 1].addG(error[1] * 1.0 / 16);
                        imageArray[x + 1][y + 1].addB(error[2] * 1.0 / 16);
                    }
                }
            }
        }

        return dithered;
    }

    private int getClosest(DitherColor color, int[][] palette) {
        double closest = Double.MAX_VALUE;
        int best = 0;

        for (int i = 0; i < palette.length; i++) {
            double heuristic =
                Math.pow(color.getR() - palette[i][2], 2) +
                    Math.pow(color.getG() - palette[i][3], 2) +
                    Math.pow(color.getB() - palette[i][4], 2);

            if (heuristic < closest) {
                closest = heuristic;
                best = i;
            }
        }

        return best;
    }

    private double[] getDifference(DitherColor one, DitherColor two) {
        return new double[]{
            one.getR() - two.getR(),
            one.getG() - two.getG(),
            one.getB() - two.getB()
        };
    }

}

