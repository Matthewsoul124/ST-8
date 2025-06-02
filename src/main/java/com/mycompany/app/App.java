package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class App {
    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "C:\\Users\\user\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions настройки = new ChromeOptions();
        настройки.addArguments("--start-maximized");
        настройки.addArguments("--remote-allow-origins=*");

        String путьЗагрузки = "D:\\Games\\testing\\ST-8\\result";
        new File(путьЗагрузки).mkdirs();

        Map<String, Object> параметры = new HashMap<>();
        параметры.put("download.default_directory", путьЗагрузки);
        параметры.put("download.prompt_for_download", false);
        параметры.put("plugins.always_open_pdf_externally", true);
        настройки.setExperimentalOption("prefs", параметры);

        WebDriver браузер = new ChromeDriver(настройки);
        WebDriverWait ожидание = new WebDriverWait(браузер, Duration.ofSeconds(30));

        try {

            браузер.get("http://www.papercdcase.com/index.php");
            System.out.println("Открыта страница: " + браузер.getTitle());


            заполнитьФорму(браузер, ожидание, "data/data.txt");


            нажатьГенерацию(браузер, ожидание);


            ждатьPDF(путьЗагрузки, 30);

        } catch (Exception e) {
            System.err.println("Ошибка при выполнении:");
            e.printStackTrace();
            сделатьСкриншот(браузер, "ошибка.png");
        } finally {
            браузер.quit();
        }
    }

    private static void заполнитьФорму(WebDriver браузер, WebDriverWait ожидание, String файлДанных) throws Exception {
        List<String> строки = Files.readAllLines(Paths.get(файлДанных));
        String артист = строки.get(0).split(": ")[1];
        String название = строки.get(1).split(": ")[1];
        List<String> треки = строки.subList(3, строки.size());


        WebElement полеАртиста = ожидание.until(ExpectedConditions.elementToBeClickable(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));
        полеАртиста.clear();
        полеАртиста.sendKeys(артист);


        WebElement полеНазвания = браузер.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
        полеНазвания.clear();
        полеНазвания.sendKeys(название);


        for (int i = 0; i < Math.min(треки.size(), 16); i++) {
            int номерСтроки = (i < 8) ? (i + 1) : (i - 7);
            String столбец = (i < 8) ? "1" : "2";

            String xpath = String.format(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%s]/table/tbody/tr[%d]/td[2]/input",
                    столбец, номерСтроки);

            WebElement полеТрека = браузер.findElement(By.xpath(xpath));
            полеТрека.clear();
            полеТрека.sendKeys(треки.get(i));
        }


        WebElement джьюелБокс = браузер.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[@value='jewel']"));
        if (!джьюелБокс.isSelected()) {
            джьюелБокс.click();
        }


        WebElement а4 = браузер.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[@value='a4']"));
        if (!а4.isSelected()) {
            а4.click();
        }

        System.out.println("Форма заполнена успешно");
    }

    private static void нажатьГенерацию(WebDriver браузер, WebDriverWait ожидание) {
        WebElement кнопка = ожидание.until(ExpectedConditions.elementToBeClickable(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")));

        ((JavascriptExecutor) браузер).executeScript("arguments[0].scrollIntoView(true);", кнопка);
        ((JavascriptExecutor) браузер).executeScript("arguments[0].click();", кнопка);

        System.out.println("Нажата кнопка генерации");
    }

    private static void ждатьPDF(String папка, int таймаут) throws Exception {
        File директория = new File(папка);
        long конец = System.currentTimeMillis() + (таймаут * 1000);

        while (System.currentTimeMillis() < конец) {
            File[] файлы = директория.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".pdf") &&
                            !name.contains(".crdownload"));

            if (файлы != null && файлы.length > 0) {
                File pdf = файлы[0];
                if (pdf.exists() && pdf.length() > 0) {
                    System.out.println("Файл сохранён: " + pdf.getName());
                    return;
                }
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException("Файл не загружен за " + таймаут + " секунд");
    }

    private static void сделатьСкриншот(WebDriver браузер, String имя) {
        try {
            File исходник = ((TakesScreenshot) браузер).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(исходник, new File(имя));
            System.out.println("Скриншот сохранён: " + имя);
        } catch (Exception e) {
            System.err.println("Не удалось сделать скриншот");
        }
    }
}