package ru.zurbag;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.zurbag.entity.ImageFile;
import ru.zurbag.repo.ImageFileRepo;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(App.class);
        ImageFileRepo imageFileRepo = context.getBean(ImageFileRepo.class);

        Scanner console = new Scanner(System.in);

        String helloText = "\n* Приложение предназначено для сжатия jpg файлов в выбранном каталоге путем изменения " +
                "компрессии файла *\n" +
                "* Путь к файлам и степень компрессии можно задать в файле config\\folder.txt * " +
                "\nВведите в консоль число соответсвующее необходимой операции: " +
                "\n1 - сканировать файлы\n2 - сжать файлы\n0 - выход из приложения ";

        System.out.println(helloText);
        int action = 0;
        while (true){
            System.out.print("Введите число: ");

            try {
                action = console.nextInt();
            }catch (Exception e){
                System.out.println("Кисо, куку, ты головой уебался? Я прошу ввести число!!! Ты точно айтишник?");
            }


            if (action == 1) {
                System.out.println("Выполняется сканирование и добавление в базу данных");
                System.out.println("Источник :" + getConfigParams().get(0));
                Collection<File> files = getFilesFromServer(getConfigParams().get(0));
                files.stream().forEach(file -> {
                    //Если файл уже есть в базе пропускаю, если нет добавляю новый, ставлю флаг отсутствия сжатия
                    if (imageFileRepo.getByName(file.getName()) == null) {
                        System.out.println("Был найден новый файл " + file.getPath());
                        imageFileRepo.save(new ImageFile(file.getPath(), file.getName(), false));
                    }
                });
                System.out.println("Сканирование файлов завершено");
                System.out.println("-----------------------");

            }
            if (action == 2) {
                System.out.println("Запущено сжатие фалов");
                List<ImageFile> filesFromDb = imageFileRepo.getByIsCompressed(false);

                AtomicReference<Integer> countFiles = new AtomicReference<>(filesFromDb.size());
                filesFromDb.stream().forEach(imageFile -> {
                    try {
                        System.out.println(" Файл: " + countFiles.getAndSet(countFiles.get() - 1) + " " + imageFile.getPath());
                        compress(new File(imageFile.getPath()));

                        //TODO вытаскиваю файл из базы данных ставлю флаг распознано
                        imageFile.setIsCompressed(true);
                        imageFileRepo.save(imageFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                System.out.println("Cжатие файлов завершено");
                System.out.println("-----------------------");

            }
            if (action == 0) {
                System.exit(0);
            }

            System.out.println(helloText);
        }

    }


    private static Collection<File> getFilesFromServer(String path) {
        return FileUtils.listFiles(new File(path), new String[]{"jpg"}, true);
    }

    private static List<String> getConfigParams() {
        List<String> paths = new ArrayList<>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("./config/folder.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("./config/folder.txt\"  - файл не найден");
        }
        //Добавляем пути
        while (scanner.hasNext()) {
            paths.add(scanner.next().trim());
        }
        scanner.close();
        return paths;
    }

    public static void compress(File input) throws IOException {
        BufferedImage image = resize(ImageIO.read(input));
        OutputStream fileOutputStream = new FileOutputStream(input);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = (ImageWriter) writers.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(fileOutputStream);
        writer.setOutput(imageOutputStream);
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        //float compressionQuality = 0.3f;
        float compressionQuality = Float.parseFloat(getConfigParams().get(1));
        param.setCompressionQuality(compressionQuality);
        writer.write(null, new IIOImage(image, null, null), param);
        fileOutputStream.close();
        imageOutputStream.close();
        writer.dispose();
    }

    public static BufferedImage resize(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        //1216 x 1728
        Image tmp = null;
        BufferedImage dimg = null;

        //Альбомный
        if (width > height) {
            if (height > 1216) {
                tmp = image.getScaledInstance(1728, 1216, Image.SCALE_SMOOTH);
                dimg = new BufferedImage(1728, 1216, BufferedImage.TYPE_INT_RGB);
            } else {
                tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }
        }
        //Книжный
        else if (width < height) {
            if (width > 1216) {
                tmp = image.getScaledInstance(1216, 1728, Image.SCALE_SMOOTH);
                dimg = new BufferedImage(1216, 1728, BufferedImage.TYPE_INT_RGB);
            } else {
                tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }
        }

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

}
