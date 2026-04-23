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
 * Representa una foto. Extrae metadata EXIF usando metadata-extractor,
 * ya que ffprobe no lee GPS de imágenes estáticas.
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

            // --- Dimensiones ---
            JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDir != null) {
                this.width  = jpegDir.getImageWidth();
                this.height = jpegDir.getImageHeight();
            }

            // --- Fecha ---
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

            // --- GPS ---
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                com.drew.lang.GeoLocation geo = gpsDir.getGeoLocation();
                if (geo != null && !geo.isZero()) {
                    this.latitude  = geo.getLatitude();
                    this.longitude = geo.getLongitude();
                }
            }

            // Nombre del archivo como nombre del medio
            this.name = this.path.getFileName().toString();

        } catch (Exception e) {
            System.err.println("Error leyendo metadata de foto: " + this.path);
            e.printStackTrace();
        }
    }
}