package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="faktura.gui.tests", matches="true")
class GuiRegressionTest {
    @TempDir Path ordner;
    @BeforeAll static void start() throws Exception {
        CountDownLatch bereit = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); bereit.countDown(); });
        assertTrue(bereit.await(10, TimeUnit.SECONDS));
    }
    @AfterAll static void stop() { Platform.exit(); }
    private static void fx(Runnable arbeit) throws Exception {
        FutureTask<Void> aufgabe = new FutureTask<>(arbeit, null); Platform.runLater(aufgabe);
        aufgabe.get(20, TimeUnit.SECONDS);
    }

    @Test @DisplayName("GUI-REM-01: Ungültiger Datumstext über Commit und Fokuswechsel bleibt sichtbar")
    void datumBleibt() throws Exception {
        DatePicker[] feld = new DatePicker[1]; Stage[] fenster = new Stage[1];
        fx(() -> {
            feld[0] = Dialoge.datumsfeld(LocalDate.of(2026, 2, 1));
            Button weiter = new Button("Weiter"); fenster[0] = new Stage();
            fenster[0].setScene(new Scene(new VBox(feld[0], weiter))); fenster[0].show();
            feld[0].getEditor().setText("31.02.2026"); feld[0].commitValue(); weiter.requestFocus();
        });
        fx(() -> {
            assertEquals("31.02.2026", feld[0].getEditor().getText());
            assertThrows(ValidierungsException.class, () -> Dialoge.datum(feld[0], "Leistungsdatum"));
            feld[0].getEditor().setText("28.2.26");
            assertEquals(LocalDate.of(2026, 2, 28), Dialoge.datum(feld[0], "Leistungsdatum"));
            fenster[0].close();
        });
    }

    @Test @DisplayName("GUI-REM-02: Zahlen und Daten sortieren fachlich, leere Daten immer zuletzt")
    void sortierung() throws Exception {
        fx(() -> {
            TableView<BigDecimal> zahlen = new TableView<>(); TableColumn<BigDecimal, BigDecimal> betrag = new TableColumn<>();
            betrag.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue()));
            zahlen.getColumns().add(betrag); Bausteine.passeSpaltenAn(zahlen);
            zahlen.getItems().addAll(new BigDecimal("1000"), new BigDecimal("9"), new BigDecimal("80"));
            zahlen.getSortOrder().add(betrag); zahlen.sort(); assertEquals(new BigDecimal("9"), zahlen.getItems().get(0));
            TableView<LocalDate> daten = new TableView<>(); TableColumn<LocalDate, LocalDate> datum = new TableColumn<>();
            datum.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue())); daten.getColumns().add(datum); Bausteine.passeSpaltenAn(daten);
            daten.getItems().addAll(null, LocalDate.of(2026, 2, 1), LocalDate.of(2025, 12, 31)); daten.getSortOrder().add(datum);
            daten.sort(); assertEquals(LocalDate.of(2025, 12, 31), daten.getItems().get(0)); assertNull(daten.getItems().get(2));
            datum.setSortType(TableColumn.SortType.DESCENDING); daten.sort(); assertNull(daten.getItems().get(2));
        });
    }

    @Test @DisplayName("GUI-REM-03: Einstellungen schließen mit Speichern, Verwerfen, Abbrechen und Validierungsfehler")
    void einstellungen() throws Exception {
        try (var k = new SpringApplicationBuilder(FakturaApplication.class).headless(false)
                .run("--faktura.daten-verzeichnis=" + ordner, "--spring.main.banner-mode=off")) {
            fx(() -> {
                var ansicht = k.getBean(FxmlLader.class).lade("einstellungen_ansicht");
                Stage stage = new Stage(); stage.setScene(new Scene(ansicht)); stage.show();
                ((TextField) ansicht.lookup("#nameFeld")).setText("Testfirma");
                var schutz = k.getBean(SchliessSchutz.class);
                waehle("Abbrechen"); assertFalse(schutz.darfSchliessen());
                waehle("Speichern"); assertFalse(schutz.darfSchliessen());
                waehle("Verwerfen"); assertTrue(schutz.darfSchliessen());
                ((TextField) ansicht.lookup("#strasseFeld")).setText("Weg 1");
                ((TextField) ansicht.lookup("#plzFeld")).setText("68163");
                ((TextField) ansicht.lookup("#ortFeld")).setText("Mannheim");
                waehle("Speichern"); assertTrue(schutz.darfSchliessen());
                assertEquals("", ((Label) ansicht.lookup("#ungespeichertAnzeige")).getText());
                assertTrue(schutz.darfSchliessen());
                // Ein echter Persistenzfehler darf weder die Änderung verwerfen noch schließen.
                ((TextField) ansicht.lookup("#nameFeld")).setText("Geänderte Testfirma");
                k.getBean(org.springframework.jdbc.core.JdbcTemplate.class).execute("DROP TABLE firmenprofil");
                waehle("Speichern"); assertFalse(schutz.darfSchliessen());
                assertEquals("Geänderte Testfirma", ((TextField) ansicht.lookup("#nameFeld")).getText());
                assertFalse(((Label) ansicht.lookup("#ungespeichertAnzeige")).getText().isEmpty());
                waehle("Abbrechen"); assertFalse(schutz.darfSchliessen());
                stage.close();
            });
        }
    }

    @Test @DisplayName("GUI-REM-04: Beenden fragt auch nach, wenn die Einstellungen gerade nicht angezeigt werden")
    void einstellungenNichtAngezeigt() throws Exception {
        try (var k = new SpringApplicationBuilder(FakturaApplication.class).headless(false)
                .run("--faktura.daten-verzeichnis=" + ordner, "--spring.main.banner-mode=off")) {
            fx(() -> {
                var ansicht = k.getBean(FxmlLader.class).lade("einstellungen_ansicht");
                Stage stage = new Stage(); Scene szene = new Scene(ansicht); stage.setScene(szene); stage.show();
                ((TextField) ansicht.lookup("#nameFeld")).setText("Testfirma");
                // Wie ein Seitenwechsel in der Hauptansicht: Die Ansicht ist aus der Szene gelöst
                szene.setRoot(new VBox());
                assertNull(ansicht.getScene());
                var schutz = k.getBean(SchliessSchutz.class);
                waehle("Abbrechen"); assertFalse(schutz.darfSchliessen());
                waehle("Verwerfen"); assertTrue(schutz.darfSchliessen());
                stage.close();
            });
        }
    }

    private static void waehle(String text) {
        Platform.runLater(() -> {
            for (Window fenster : List.copyOf(Window.getWindows())) {
                if (fenster.getScene().lookup(".dialog-pane") instanceof DialogPane pane) {
                    if (text.equals("Speichern")) { Platform.runLater(() -> waehle("OK")); }
                    pane.getButtonTypes().stream().filter(b -> b.getText().equals(text)).findFirst()
                            .ifPresent(b -> ((Button) pane.lookupButton(b)).fire());
                }
            }
        });
    }
}
