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
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@SpringBootApplication
public class App {


    public static void main(String[] args) throws InterruptedException {
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
        while (true) {
            System.out.print("Введите число: ");

            try {
                action = console.nextInt();
            } catch (Exception e) {
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
                long startTime = System.currentTimeMillis();
                System.out.println(LocalDateTime.now());
                List<ImageFile> filesFromDb = imageFileRepo.findFirst200000ByIsCompressed(false);
                System.out.println(filesFromDb.size());

                int size = filesFromDb.size() / 3;
                int th1Start = 0;
                int th1Stop = th1Start+size;
                int th2Start = th1Stop + 1;
                int th2Stop = th2Start+size;
                int th3Start = th2Stop + 1;
                int th3Stop = filesFromDb.size();

                Thread thread1 = createThread(imageFileRepo, th1Start, th1Stop, filesFromDb, "Thread - 1");
                Thread thread2 = createThread(imageFileRepo, th2Start, th2Stop, filesFromDb, "Thread - 2");
                Thread thread3 = createThread(imageFileRepo, th3Start, th3Stop, filesFromDb, "Thread - 3");

                thread1.start();
                thread2.start();
                thread3.start();
                thread1.join();
                thread2.join();
                thread3.join();

                //TODO аккуратнее рабочий код
//                AtomicReference<Integer> countFiles = new AtomicReference<>(filesFromDb.size());
//                filesFromDb.stream().forEach(imageFile -> {
//                    try {
//                        System.out.println(" Файл: " + countFiles.getAndSet(countFiles.get() - 1) + " " + imageFile.getPath());
//                        try {
//                            compress(new File(imageFile.getPath()));
//                        } catch (Exception e) {
//                            System.out.println(e);
//                            System.out.println("Вообще пофиг 1 файл погоды не делает");
//                        }
//
//                        imageFile.setIsCompressed(true);
//                        imageFileRepo.save(imageFile);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                });

                long stopTime = System.currentTimeMillis();
                System.out.println(LocalDateTime.now());
                System.out.println((stopTime - startTime) / 1000 + " секунд");
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


    public static Thread createThread(ImageFileRepo imageFileRepo, int start, int stop, List<ImageFile> filesFromDb, String threadName) {
        Thread thread = new Thread(() -> {
            for (int i = start; i < stop; i++) {
                try {
                    try {
                        System.out.println(threadName+" - "+filesFromDb.get(i).getId() + " - " + filesFromDb.get(i).getPath());
                        compress(new File(filesFromDb.get(i).getPath()));
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Вообще пофиг 1 файл погоды не делает");
                    }

                    filesFromDb.get(i).setIsCompressed(true);
                    imageFileRepo.save(filesFromDb.get(i));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return thread;
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
