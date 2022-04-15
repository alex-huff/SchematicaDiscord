package phonis.SchematicaDownload.util;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

import java.io.IOException;
import java.io.InputStream;

public class SchemStreamToClipboard implements ClipboardConverter
{

    @Override
    public Clipboard getClipboard(InputStream stream) throws ClipboardException
    {
        ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
        ClipboardReader reader;

        try
        {
            reader = format.getReader(stream);
        }
        catch (IOException e)
        {
            throw new ClipboardException("IOException during loading of schematic.");
        }

        Clipboard clipboard;

        try
        {
            clipboard = reader.read();

            stream.close();
        }
        catch (IOException e)
        {
            throw new ClipboardException("IOException during load to clipboard.");
        }

        return clipboard;
    }

}
