package com.muravlev.cloudstorage.lesson01;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            System.out.printf("Client %s connected\n", socket.getInetAddress());
            while (true) {
                String command = in.readUTF();
                if ("upload".equals(command)) {
                    try {
                        File file = new File("server"  + File.separator + in.readUTF());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream fos = new FileOutputStream(file);

                        long size = in.readLong();
                        System.out.println(size);

                        byte[] buffer = new byte[8 * 1024];
                        System.out.println(buffer.length-1);
                        //не совсем понятно, не вижу разницы между (size + buffer.length) / (buffer.length) и тем, что
                        //представлено, загнал всё в цифры, но не могу понять принципиальную разницу в отсутствии одного байта
                        //допускаю, что это может быть сделано для оптимизации кол-ва итераций в цикле, ведь если написать просто
                        //i < size, то это может занять время. Ведь у нас буффер фиксированного размера и кол-во итераций цикла
                        //должно уменьшаться от размера файла и величины буффера.В случае (size + buffer.length) / (buffer.length)
                        //мы всегда получим число большее 1. Одной итерации достаточно для маленького файла.
                        //Чую, что ответ на поверхности, но я его в упор не вижу. Подумаю ещё.
                        for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) {
                            int read = in.read(buffer);
                            fos.write(buffer, 0, read);
                        }
                        fos.close();

                        out.writeUTF("OK");

                    } catch (Exception e) {
                        out.writeUTF("FATAL ERROR");
                    }
                }

                if ("download".equals(command)) {
                    // TODO: 14.06.2021
                    //1. Выполнить задания по TODO (2 штуки) (организовать скачивание файлов с сервера)
                    //эм... тут же даже менять ничего не надо толком? просто взял и скопировал кусок -_-
                    try {
                        File file = new File("client"  + File.separator + in.readUTF());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream fos = new FileOutputStream(file);

                        long size = in.readLong();

                        byte[] buffer = new byte[8 * 1024];

                        for (int i = 0; i < (size + (buffer.length - 1)) / (buffer.length); i++) {
                            int read = in.read(buffer);
                            fos.write(buffer, 0, read);
                        }
                        fos.close();
                        out.writeUTF("OK");
                    } catch (Exception e) {
                        out.writeUTF("FATAL ERROR");
                    }

                }
                if ("exit".equals(command)) {
                    System.out.printf("Client %s disconnected correctly\n", socket.getInetAddress());
                    socket.close();
                    break;
                }
               // 2. Организовать корректный вывод статуса скачивания и отправки файлов.
                // Просто убираем ещё один out.writeUTF и тогда он перестанет их чередовать
//                System.out.println(command);
//                out.writeUTF(command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
