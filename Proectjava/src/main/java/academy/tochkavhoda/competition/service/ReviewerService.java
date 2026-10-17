package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.Storage;
import academy.tochkavhoda.competition.dto.request.LoginDtoRequest;
import academy.tochkavhoda.competition.dto.request.RateApplicationDtoRequest;
import academy.tochkavhoda.competition.dto.request.RegisterExpertDtoRequest;
import academy.tochkavhoda.competition.dto.response.LoginDtoResponse;
import academy.tochkavhoda.competition.model.AuthToken;
import academy.tochkavhoda.competition.model.GrantRequest;
import academy.tochkavhoda.competition.model.Reviewer;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for reviewers (experts evaluating grant requests).
 */
public class ReviewerService {
    private final Storage storage;
    private final Gson gson = new Gson();

    public ReviewerService(Storage storage) {
        this.storage = storage;
    }

    public ServerResponse registerReviewer(String jsonPayload) {
        try {
            RegisterExpertDtoRequest dto = gson.fromJson(jsonPayload, RegisterExpertDtoRequest.class);
            checkRegistrationPayload(dto);

            String loginKey = dto.getLogin().trim();
            if (storage.lookupContenderByLogin(loginKey).isPresent()
                    || storage.lookupReviewerByLogin(loginKey).isPresent()) {
                return ServerResponse.error("Этот логин уже используется");
            }

            List<String> areas = dto.getDirections().stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            Reviewer entity = new Reviewer(
                    0,
                    dto.getFirstName().trim(),
                    dto.getLastName().trim(),
                    areas,
                    loginKey,
                    dto.getPassword()
            );
            storage.persistReviewer(entity);
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse authenticate(String jsonPayload) {
        try {
            LoginDtoRequest dto = gson.fromJson(jsonPayload, LoginDtoRequest.class);
            InputChecker.checkLoginFormat(dto.getLogin());
            InputChecker.requireNonBlank(dto.getPassword(), "Пароль");

            Reviewer found = storage.lookupReviewerByLogin(dto.getLogin().trim()).orElse(null);
            if (found == null || !found.getCredentialsSecret().equals(dto.getPassword())) {
                return ServerResponse.error("Неверный логин или пароль");
            }

            String tokenStr = UUID.randomUUID().toString();
            AuthToken session = new AuthToken(tokenStr, found.getIdentifier(), AuthToken.Role.REVIEWER);
            storage.storeToken(session);

            return ServerResponse.success(gson.toJson(new LoginDtoResponse(tokenStr)));
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse signOut(String tokenStr) {
        try {
            resolveReviewerSession(tokenStr);
            storage.invalidateToken(tokenStr);
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    public ServerResponse withdrawCompletely(String tokenStr) {
        try {
            AuthToken session = resolveReviewerSession(tokenStr);
            storage.eraseReviewer(session.getLinkedUserId());
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    public ServerResponse assignScore(String tokenStr, String jsonPayload) {
        try {
            if (storage.isContestFinished()) {
                return ServerResponse.error("Конкурс закрыт, оценка заявок невозможна");
            }
            AuthToken session = resolveReviewerSession(tokenStr);
            RateApplicationDtoRequest dto = gson.fromJson(jsonPayload, RateApplicationDtoRequest.class);
            InputChecker.checkScoreRange(dto.getScore());

            GrantRequest request = storage.lookupGrantRequestById(dto.getApplicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

            Reviewer reviewer = storage.lookupReviewerById(session.getLinkedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Эксперт не найден"));

            // Check that at least one subject area matches expertise
            boolean areaMatch = request.getSubjectAreas().stream()
                    .map(String::toLowerCase)
                    .anyMatch(area -> reviewer.getExpertiseAreas().stream()
                            .map(String::toLowerCase)
                            .anyMatch(area::equals));
            if (!areaMatch) {
                return ServerResponse.error("Эксперт может оценивать заявки только по своим направлениям");
            }

            request.putScore(reviewer.getIdentifier(), dto.getScore());
            reviewer.markAsEvaluated(request.getIdentifier());
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse removeScore(String tokenStr, String jsonPayload) {
        try {
            AuthToken session = resolveReviewerSession(tokenStr);
            int requestId = gson.fromJson(jsonPayload, JsonObject.class).get("applicationId").getAsInt();

            GrantRequest request = storage.lookupGrantRequestById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

            request.eraseScore(session.getLinkedUserId());
            storage.lookupReviewerById(session.getLinkedUserId())
                    .ifPresent(r -> r.unmarkEvaluation(requestId));
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse listByAreas(String tokenStr, String jsonPayload) {
        try {
            AuthToken session = resolveReviewerSession(tokenStr);
            Reviewer reviewer = storage.lookupReviewerById(session.getLinkedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Эксперт не найден"));

            List<String> filterAreas;
            if (jsonPayload == null || jsonPayload.trim().isEmpty() || jsonPayload.trim().equals("{}")) {
                filterAreas = reviewer.getExpertiseAreas();
            } else {
                JsonObject obj = gson.fromJson(jsonPayload, JsonObject.class);
                if (obj.has("directions")) {
                    filterAreas = gson.fromJson(obj.get("directions"), List.class);
                } else {
                    filterAreas = reviewer.getExpertiseAreas();
                }
            }

            List<GrantRequest> matching = storage.findRequestsMatchingAreas(filterAreas);
            // Restrict to areas the reviewer is allowed to see
            List<String> allowed = reviewer.getExpertiseAreas().stream()
                    .map(String::toLowerCase).collect(Collectors.toList());
            matching = matching.stream()
                    .filter(req -> req.getSubjectAreas().stream()
                            .map(String::toLowerCase)
                            .anyMatch(allowed::contains))
                    .collect(Collectors.toList());

            String json = gson.toJson(matching.stream().map(this::toSummary).collect(Collectors.toList()));
            return ServerResponse.success(json);
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse listScoredByMe(String tokenStr) {
        try {
            AuthToken session = resolveReviewerSession(tokenStr);
            Reviewer reviewer = storage.lookupReviewerById(session.getLinkedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Эксперт не найден"));

            List<GrantRequest> scored = reviewer.getEvaluatedRequestIds().stream()
                    .map(id -> storage.lookupGrantRequestById(id).orElse(null))
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            String json = gson.toJson(scored.stream().map(this::toSummary).collect(Collectors.toList()));
            return ServerResponse.success(json);
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    private AuthToken resolveReviewerSession(String tokenStr) {
        AuthToken session = storage.lookupToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Недействительный токен"));
        if (session.getRole() != AuthToken.Role.REVIEWER) {
            throw new IllegalArgumentException("Токен не принадлежит эксперту");
        }
        return session;
    }

    private void checkRegistrationPayload(RegisterExpertDtoRequest dto) {
        InputChecker.requireMinLength(dto.getFirstName(), InputChecker.NAME_MIN_CHARS, "Имя");
        InputChecker.requireMinLength(dto.getLastName(), InputChecker.NAME_MIN_CHARS, "Фамилия");
        if (dto.getDirections() == null || dto.getDirections().isEmpty()) {
            throw new IllegalArgumentException("Список направлений не может быть пустым");
        }
        InputChecker.checkLoginFormat(dto.getLogin());
        InputChecker.checkSecretStrength(dto.getPassword());
    }

    private java.util.Map<String, Object> toSummary(GrantRequest req) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", req.getIdentifier());
        map.put("title", req.getRequestTitle());
        map.put("description", req.getRequestDescription());
        map.put("directions", req.getSubjectAreas());
        map.put("amount", req.getRequestedSum());
        map.put("averageScore", req.computeAverageScore());
        return map;
    }
}
