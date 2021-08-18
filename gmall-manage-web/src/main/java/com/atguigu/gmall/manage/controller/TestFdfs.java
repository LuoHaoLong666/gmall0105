package com.atguigu.gmall.manage.controller;

public class TestFdfs {

    public static void main(String[] args) {

        String originalFilename = "aaaas.da23131d.12312321321.jpg"; //a.jpg
        System.out.println(originalFilename);
        int i = originalFilename.lastIndexOf(".");
        String extName = originalFilename.substring(i+1);
        System.out.println(extName);
    }
}
