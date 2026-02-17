import java.io.FileWriter;
import java.io.IOException;

public class SVGGenerator {

    public static void main(String[] args) {
        generateSVG("rectangle", "rectangle.svg");
        generateSVG("sunshine", "sunshine.svg");
    }

    public static void generateSVG(String contentType, String outputPath) {

        String svgContent = buildSVGContent(contentType);

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(svgContent);
            System.out.println("SVG generado correctamente en: " + outputPath);
        } catch (IOException e) {
            System.out.println("Error al generar el archivo SVG.");
        }
    }

    private static String buildSVGContent(String contentType) {

        StringBuilder svg = new StringBuilder();

        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\">\n");

        switch (contentType.toLowerCase()) {

            case "rectangle":
                svg.append("<rect width=\"800\" height=\"600\" fill=\"blue\" />\n");
                svg.append("<polygon points=\"0,0 800,0 800,600\" fill=\"red\" />\n");
                break;

            case "sunshine":
                // Fondo gris (cielo)
                svg.append("<rect width=\"800\" height=\"600\" fill=\"#d3d3d3\" />\n");

                svg.append("<path d=\"")
                        .append("M0 450 ")
                        .append("Q50 400 100 450 ")
                        .append("T200 450 ")
                        .append("T300 450 ")
                        .append("T400 450 ")
                        .append("T500 450 ")
                        .append("T600 450 ")
                        .append("T700 450 ")
                        .append("T800 450 ")
                        .append("L800 600 L0 600 Z\" ")
                        .append("fill=\"lime\" />\n");

                svg.append("<line x1=\"150\" y1=\"30\" x2=\"150\" y2=\"90\" stroke=\"red\" />\n");
                svg.append("<line x1=\"150\" y1=\"210\" x2=\"150\" y2=\"270\" stroke=\"red\" />\n");
                svg.append("<line x1=\"30\" y1=\"150\" x2=\"90\" y2=\"150\" stroke=\"red\" />\n");
                svg.append("<line x1=\"210\" y1=\"150\" x2=\"270\" y2=\"150\" stroke=\"red\" />\n");

                svg.append("<line x1=\"70\" y1=\"70\" x2=\"110\" y2=\"110\" stroke=\"red\" />\n");
                svg.append("<line x1=\"230\" y1=\"70\" x2=\"190\" y2=\"110\" stroke=\"red\" />\n");
                svg.append("<line x1=\"70\" y1=\"230\" x2=\"110\" y2=\"190\" stroke=\"red\" />\n");
                svg.append("<line x1=\"230\" y1=\"230\" x2=\"190\" y2=\"190\" stroke=\"red\" />\n");

                svg.append("<circle cx=\"150\" cy=\"150\" r=\"70\" fill=\"yellow\" />\n");
                break;

            default:
                svg.append("<text x=\"50\" y=\"200\" font-size=\"30\" fill=\"black\">")
                        .append(contentType)
                        .append("</text>\n");
                break;
        }

        svg.append("</svg>");

        return svg.toString();
    }
}
