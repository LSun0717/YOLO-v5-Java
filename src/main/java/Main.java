import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;

import static org.opencv.videoio.Videoio.CAP_ANY;

public class Main {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        Translator<Image, DetectedObjects> translator = YoloV5Translator.builder().optSynsetArtifactName("coco.names").build();
        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(Image.class, DetectedObjects.class)
                        .optDevice(Device.gpu())
                        .optModelUrls(Main.class.getResource("/yolov5s").getPath())
                        .optModelName("yolov5s.torchscript.pt")
                        .optTranslator(translator)
                        .optEngine("PyTorch")
                        .build();
//        Criteria<Image, DetectedObjects> criteria =
//                Criteria.builder()
//                        .setTypes(Image.class, DetectedObjects.class)
//                        .optDevice(Device.cpu())
//                        .optModelUrls(Main.class.getResource("/yolov5").getPath())
//                        .optModelName("yolov5s.onnx")
//                        .optTranslator(translator)
//                        .optEngine("OnnxRuntime")
//                        .build();
        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria)) {
            VideoCapture cap = new VideoCapture(CAP_ANY);
            if (!cap.isOpened()) {//isOpened?????????????????????????????????????????????
                System.out.println("Camera Error");//????????????????????????????????????????????????
            } else {
                Mat frame = new Mat();//?????????????????????
                boolean flag = cap.read(frame);//read?????????????????????????????????
                while (flag) {
                    detect(frame, model);
                    HighGui.imshow("yolov5", frame);
                    HighGui.waitKey(20);
                    flag = cap.read(frame);
                }
            }

        } catch (RuntimeException | ModelException | TranslateException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Image mat2Image(Mat mat) {
        return ImageFactory.getInstance().fromImage(HighGui.toBufferedImage(mat));
    }


    static Rect rect = new Rect();
    static Scalar color = new Scalar(0, 255, 0);

    static void detect(Mat frame, ZooModel<Image, DetectedObjects> model) throws IOException, ModelNotFoundException, MalformedModelException, TranslateException {
        Image img = mat2Image(frame);
        long startTime = System.currentTimeMillis();
        try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects results = predictor.predict(img);
//            System.out.println(results);
            for (DetectedObject obj : results.<DetectedObject>items()) {
                BoundingBox bbox = obj.getBoundingBox();
                Rectangle rectangle = bbox.getBounds();
                String showText = String.format("%s: %.2f", obj.getClassName(), obj.getProbability());
                rect.x = (int) rectangle.getX();
                rect.y = (int) rectangle.getY();
                rect.width = (int) rectangle.getWidth();
                rect.height = (int) rectangle.getHeight();
                // ??????

                Imgproc.rectangle(frame, rect, color, 2);
                //?????????
                Imgproc.putText(frame, showText,
                        new Point(rect.x, rect.y),
                        Imgproc.FONT_HERSHEY_COMPLEX,
                        rectangle.getWidth() / 200,
                        color);
            }
        }
        System.out.printf("%.2f%n", 1000.0 / (System.currentTimeMillis() - startTime));
    }
}


