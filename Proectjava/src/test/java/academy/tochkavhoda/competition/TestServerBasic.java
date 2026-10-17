package academy.tochkavhoda.competition;

import academy.tochkavhoda.competition.server.Server;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestServerBasic {
    private Server server;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        server = new Server();
    }

    @Test
    void testRegisterAndLoginParticipant() {
        String regJson = """
                {
                  "firstName": "Михаил",
                  "lastName": "Орлов",
                  "companyName": "НПО Горизонт",
                  "login": "orlov_m",
                  "password": "Secur3#key"
                }
                """;
        ServerResponse regResp = server.registerParticipant(regJson);
        assertEquals(200, regResp.getResponseCode());

        String loginJson = """
                {
                  "login": "orlov_m",
                  "password": "Secur3#key"
                }
                """;
        ServerResponse loginResp = server.loginParticipant(loginJson);
        assertEquals(200, loginResp.getResponseCode());
        JsonObject obj = gson.fromJson(loginResp.getResponseData(), JsonObject.class);
        assertTrue(obj.has("token"));
        assertFalse(obj.get("token").getAsString().isEmpty());
    }

    @Test
    void testRegisterDuplicateLogin() {
        String regJson = """
                {
                  "firstName": "Михаил",
                  "lastName": "Орлов",
                  "companyName": "НПО Горизонт",
                  "login": "orlov_m",
                  "password": "Secur3#key"
                }
                """;
        assertEquals(200, server.registerParticipant(regJson).getResponseCode());
        ServerResponse resp2 = server.registerParticipant(regJson);
        assertEquals(400, resp2.getResponseCode());
        assertTrue(resp2.getResponseData().contains("error"));
    }

    @Test
    void testRegisterExpertAndRate() {
        // Register participant and add application
        String regPart = """
                {
                  "firstName": "Ольга",
                  "lastName": "Волкова",
                  "companyName": "Лаборатория Квант",
                  "login": "volkova_o",
                  "password": "Qk9!matrix"
                }
                """;
        assertEquals(200, server.registerParticipant(regPart).getResponseCode());

        String loginPart = """
                {"login":"volkova_o","password":"Qk9!matrix"}
                """;
        ServerResponse loginResp = server.loginParticipant(loginPart);
        String partToken = gson.fromJson(loginResp.getResponseData(), JsonObject.class).get("token").getAsString();

        String appJson = """
                {
                  "title": "Нейросетевой анализатор трафика",
                  "description": "Система автоматического выявления аномалий в сетевом трафике на базе глубокого обучения",
                  "directions": ["информатика", "математика"],
                  "amount": 1750000
                }
                """;
        ServerResponse addAppResp = server.addApplication(partToken, appJson);
        assertEquals(200, addAppResp.getResponseCode());
        int appId = gson.fromJson(addAppResp.getResponseData(), JsonObject.class).get("id").getAsInt();

        // Register expert
        String regExp = """
                {
                  "firstName": "Дмитрий",
                  "lastName": "Кузнецов",
                  "directions": ["информатика", "физика"],
                  "login": "kuznetsov_d",
                  "password": "Exp3rt$now"
                }
                """;
        assertEquals(200, server.registerExpert(regExp).getResponseCode());

        String loginExp = """
                {"login":"kuznetsov_d","password":"Exp3rt$now"}
                """;
        ServerResponse expLogin = server.loginExpert(loginExp);
        String expToken = gson.fromJson(expLogin.getResponseData(), JsonObject.class).get("token").getAsString();

        // Rate
        String rateJson = String.format("""
                {"applicationId": %d, "score": 5}
                """, appId);
        ServerResponse rateResp = server.rateApplication(expToken, rateJson);
        assertEquals(200, rateResp.getResponseCode());

        // Get rated
        ServerResponse rated = server.getRatedApplications(expToken);
        assertEquals(200, rated.getResponseCode());
        assertTrue(rated.getResponseData().contains("Нейросетевой анализатор трафика"));
    }

    @Test
    void testResultsCalculation() {
        // Setup fund and threshold
        server.setTotalFund(null, "{\"amount\": 3200000}");
        server.setThreshold(null, "{\"threshold\": 3.5}");

        // Contender 1
        ServerResponse r1 = server.registerParticipant("""
                {"firstName":"Сергей","lastName":"Морозов","companyName":"ТехноСфера","login":"morozov_s","password":"Ts2024!ok"}
                """);
        assertEquals(200, r1.getResponseCode());
        ServerResponse login1 = server.loginParticipant("{\"login\":\"morozov_s\",\"password\":\"Ts2024!ok\"}");
        assertEquals(200, login1.getResponseCode());
        String t1 = gson.fromJson(login1.getResponseData(), JsonObject.class).get("token").getAsString();
        ServerResponse app1Resp = server.addApplication(t1, """
                {"title":"Платформа умного логистики","description":"Оптимизация маршрутов доставки с использованием reinforcement learning","directions":["it"],"amount":1400000}
                """);
        assertEquals(200, app1Resp.getResponseCode());
        int app1Id = gson.fromJson(app1Resp.getResponseData(), JsonObject.class).get("id").getAsInt();

        // Contender 2
        assertEquals(200, server.registerParticipant("""
                {"firstName":"Наталья","lastName":"Белова","companyName":"АгроИнтеллект","login":"belova_n","password":"Agro#77x"}
                """).getResponseCode());
        ServerResponse login2 = server.loginParticipant("{\"login\":\"belova_n\",\"password\":\"Agro#77x\"}");
        assertEquals(200, login2.getResponseCode());
        String t2 = gson.fromJson(login2.getResponseData(), JsonObject.class).get("token").getAsString();
        ServerResponse app2Resp = server.addApplication(t2, """
                {"title":"Датчики почвенного мониторинга","description":"Сеть IoT-датчиков для точного земледелия и прогнозирования урожайности","directions":["it"],"amount":1100000}
                """);
        assertEquals(200, app2Resp.getResponseCode());
        int app2Id = gson.fromJson(app2Resp.getResponseData(), JsonObject.class).get("id").getAsInt();

        // Reviewer
        assertEquals(200, server.registerExpert("""
                {"firstName":"Игорь","lastName":"Савельев","directions":["it"],"login":"saveliev_i","password":"Rev1ew!go"}
                """).getResponseCode());
        ServerResponse expLogin = server.loginExpert("{\"login\":\"saveliev_i\",\"password\":\"Rev1ew!go\"}");
        assertEquals(200, expLogin.getResponseCode());
        String et = gson.fromJson(expLogin.getResponseData(), JsonObject.class).get("token").getAsString();

        // Rate first request = 5, second = 4
        assertEquals(200, server.rateApplication(et, String.format("{\"applicationId\":%d,\"score\":5}", app1Id)).getResponseCode());
        assertEquals(200, server.rateApplication(et, String.format("{\"applicationId\":%d,\"score\":4}", app2Id)).getResponseCode());

        // Close and calculate
        ServerResponse results = server.closeAndCalculateResults(null);
        assertEquals(200, results.getResponseCode());
        assertTrue(results.getResponseData().contains("Платформа умного логистики"));
    }
}
