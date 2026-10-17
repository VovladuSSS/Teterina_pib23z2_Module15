package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.Storage;
import academy.tochkavhoda.competition.model.GrantRequest;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Handles contest-wide operations: fund configuration, closing, ranking and results.
 */
public class GrantService {
    private final Storage storage;
    private final Gson gson = new Gson();
    private final Random shuffleSource = new Random();

    public GrantService(Storage storage) {
        this.storage = storage;
    }

    public ServerResponse configureFundSize(String token, String jsonPayload) {
        try {
            var payload = gson.fromJson(jsonPayload, java.util.Map.class);
            long size = ((Number) payload.get("amount")).longValue();
            if (size < 0) {
                return ServerResponse.error("Сумма фонда не может быть отрицательной");
            }
            storage.updateTotalFund(size);
            return ServerResponse.success();
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse configureScoreCutoff(String token, String jsonPayload) {
        try {
            var payload = gson.fromJson(jsonPayload, java.util.Map.class);
            double cutoff = ((Number) payload.get("threshold")).doubleValue();
            if (cutoff < 0 || cutoff > 5) {
                return ServerResponse.error("Пороговое значение должно быть от 0 до 5");
            }
            storage.updateScoreThreshold(cutoff);
            return ServerResponse.success();
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse finalizeRanking(String token) {
        try {
            if (storage.isContestFinished()) {
                return ServerResponse.error("Конкурс уже закрыт");
            }
            storage.markContestFinished();

            List<GrantRequest> candidates = new ArrayList<>(storage.fetchAllGrantRequests());

            // Primary: higher average score first
            // Secondary: smaller requested sum first
            // Tertiary: random order
            candidates.sort((left, right) -> {
                int byScore = Double.compare(right.computeAverageScore(), left.computeAverageScore());
                if (byScore != 0) {
                    return byScore;
                }
                int bySum = Long.compare(left.getRequestedSum(), right.getRequestedSum());
                if (bySum != 0) {
                    return bySum;
                }
                return shuffleSource.nextInt(3) - 1;
            });

            long leftover = storage.readTotalFund();
            double cutoff = storage.readScoreThreshold();
            List<GrantRequest> selected = new ArrayList<>();

            for (GrantRequest candidate : candidates) {
                double avg = candidate.computeAverageScore();
                if (avg <= cutoff) {
                    break;
                }
                if (candidate.getRequestedSum() <= leftover) {
                    selected.add(candidate);
                    leftover -= candidate.getRequestedSum();
                }
            }

            storage.writeWinningRequests(selected);
            return ServerResponse.success(gson.toJson(selected));
        } catch (Exception ex) {
            return ServerResponse.error("Ошибка при подведении итогов: " + ex.getMessage());
        }
    }

    public ServerResponse fetchFinalResults(String token) {
        try {
            if (!storage.isContestFinished()) {
                return ServerResponse.error("Итоги еще не подведены");
            }
            return ServerResponse.success(gson.toJson(storage.readWinningRequests()));
        } catch (Exception ex) {
            return ServerResponse.error("Ошибка");
        }
    }

    public ServerResponse listAllContenders(String token, String jsonPayload) {
        try {
            return ServerResponse.success(gson.toJson(storage.fetchAllContenders()));
        } catch (Exception ex) {
            return ServerResponse.error("Ошибка");
        }
    }

    public ServerResponse listAllRequests(String token, String jsonPayload) {
        try {
            return ServerResponse.success(gson.toJson(storage.fetchAllGrantRequests()));
        } catch (Exception ex) {
            return ServerResponse.error("Ошибка");
        }
    }
}
