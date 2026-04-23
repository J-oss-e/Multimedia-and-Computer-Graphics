package com.josse;

import java.io.File;
import java.time.ZoneId;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

/**
 * Represents a still image (JPG, PNG, WEBP).
 * Uses metadata-extractor for EXIF parsing because ffprobe does not expose
 * GPS tags from static image files.
 */
public class Photo extends VisualMedia {

    public Photo() {
        super();
        this.type = dataType.PHOTO;
    }

    @Override
    public void extractMetadata(FFmpegProcessor ffmpeg) {
        try {
            File file = this.path.toFile();
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // JpegDirectory only exists for JPEG files; PNG/WEBP will leave width/height at 0.
            JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDir != null) {
                this.width  = jpegDir.getImageWidth();
                this.height = jpegDir.getImageHeight();
            }

            // TAG_DATETIME_ORIGINAL is the shutter-press timestamp; avoids using
            // TAG_DATETIME which reflects the last edit time instead.
            ExifSubIFDDirectory exifDir =
                metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                Date date = exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    this.date = date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                }
            }

            // geo.isZero() guards against cameras that write 0,0 when GPS is unavailable.
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                com.drew.lang.GeoLocation geo = gpsDir.getGeoLocation();
                if (geo != null && !geo.isZero()) {
                    this.latitude  = geo.getLatitude();
                    this.longitude = geo.getLongitude();
                }
            }

            this.name = this.path.getFileName().toString();

        } catch (Exception e) {
            System.err.println("Error leyendo metadata de foto: " + this.path);
            e.printStackTrace();
        }
    }
}