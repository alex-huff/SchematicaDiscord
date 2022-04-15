package phonis.SchematicaDownload.util;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.InputStream;

public interface ClipboardConverter
{

    Clipboard getClipboard(InputStream stream) throws ClipboardException;

}
