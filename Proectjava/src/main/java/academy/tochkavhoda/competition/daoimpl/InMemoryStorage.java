package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.Storage;
import academy.tochkavhoda.competition.model.AuthToken;
import academy.tochkavhoda.competition.model.Contender;
import academy.tochkavhoda.competition.model.GrantRequest;
import academy.tochkavhoda.competition.model.Reviewer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory implementation of the storage layer.
 */
public class InMemoryStorage implements Storage {

    private final Map<Integer, Contender> contenderRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, Reviewer> reviewerRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, GrantRequest> requestRegistry = new ConcurrentHashMap<>();
    private final Map<String, AuthToken> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> loginIndexContenders = new ConcurrentHashMap<>();
    private final Map<String, Integer> loginIndexReviewers = new ConcurrentHashMap<>();

    private final AtomicInteger contenderSeq = new AtomicInteger(1);
    private final AtomicInteger reviewerSeq = new AtomicInteger(1);
    private final AtomicInteger requestSeq = new AtomicInteger(1);

    private long grantPoolSize = 0L;
    private double minimumScore = 0.0;
    private boolean contestClosedFlag = false;
    private List<GrantRequest> selectedWinners = new ArrayList<>();

    @Override
    public Contender persistContender(Contender contender) {
        int newId = allocateContenderId();
        contender.setIdentifier(newId);
        contenderRegistry.put(newId, contender);
        loginIndexContenders.put(contender.getCredentialsLogin(), newId);
        return contender;
    }

    @Override
    public Optional<Contender> lookupContenderById(int id) {
        return Optional.ofNullable(contenderRegistry.get(id));
    }

    @Override
    public Optional<Contender> lookupContenderByLogin(String login) {
        Integer foundId = loginIndexContenders.get(login);
        if (foundId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(contenderRegistry.get(foundId));
    }

    @Override
    public Collection<Contender> fetchAllContenders() {
        return new ArrayList<>(contenderRegistry.values());
    }

    @Override
    public void eraseContender(int id) {
        Contender removed = contenderRegistry.remove(id);
        if (removed != null) {
            loginIndexContenders.remove(removed.getCredentialsLogin());
            for (Integer reqId : new ArrayList<>(removed.getSubmittedRequestIds())) {
                eraseGrantRequest(reqId);
            }
            invalidateTokensForUser(id, AuthToken.Role.CONTENDER);
        }
    }

    @Override
    public Reviewer persistReviewer(Reviewer reviewer) {
        int newId = allocateReviewerId();
        reviewer.setIdentifier(newId);
        reviewerRegistry.put(newId, reviewer);
        loginIndexReviewers.put(reviewer.getCredentialsLogin(), newId);
        return reviewer;
    }

    @Override
    public Optional<Reviewer> lookupReviewerById(int id) {
        return Optional.ofNullable(reviewerRegistry.get(id));
    }

    @Override
    public Optional<Reviewer> lookupReviewerByLogin(String login) {
        Integer foundId = loginIndexReviewers.get(login);
        if (foundId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reviewerRegistry.get(foundId));
    }

    @Override
    public Collection<Reviewer> fetchAllReviewers() {
        return new ArrayList<>(reviewerRegistry.values());
    }

    @Override
    public void eraseReviewer(int id) {
        Reviewer removed = reviewerRegistry.remove(id);
        if (removed != null) {
            loginIndexReviewers.remove(removed.getCredentialsLogin());
            for (GrantRequest req : requestRegistry.values()) {
                req.eraseScore(id);
            }
            invalidateTokensForUser(id, AuthToken.Role.REVIEWER);
        }
    }

    @Override
    public GrantRequest persistGrantRequest(GrantRequest request) {
        int newId = allocateRequestId();
        request.setIdentifier(newId);
        requestRegistry.put(newId, request);
        lookupContenderById(request.getOwnerContenderId())
                .ifPresent(c -> c.attachRequestId(newId));
        return request;
    }

    @Override
    public Optional<GrantRequest> lookupGrantRequestById(int id) {
        return Optional.ofNullable(requestRegistry.get(id));
    }

    @Override
    public Collection<GrantRequest> fetchAllGrantRequests() {
        return new ArrayList<>(requestRegistry.values());
    }

    @Override
    public void eraseGrantRequest(int id) {
        GrantRequest removed = requestRegistry.remove(id);
        if (removed != null) {
            lookupContenderById(removed.getOwnerContenderId())
                    .ifPresent(c -> c.detachRequestId(id));
            for (Integer reviewerId : removed.getEvaluationScores().keySet()) {
                lookupReviewerById(reviewerId)
                        .ifPresent(r -> r.unmarkEvaluation(id));
            }
        }
    }

    @Override
    public List<GrantRequest> findRequestsMatchingAreas(Collection<String> areas) {
        if (areas == null || areas.isEmpty()) {
            return new ArrayList<>(requestRegistry.values());
        }
        Set<String> normalized = areas.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return requestRegistry.values().stream()
                .filter(req -> req.getSubjectAreas().stream()
                        .map(String::toLowerCase)
                        .anyMatch(normalized::contains))
                .collect(Collectors.toList());
    }

    @Override
    public List<GrantRequest> findRequestsOwnedBy(int contenderId) {
        return requestRegistry.values().stream()
                .filter(req -> req.getOwnerContenderId() == contenderId)
                .collect(Collectors.toList());
    }

    @Override
    public void storeToken(AuthToken token) {
        activeSessions.put(token.getTokenValue(), token);
    }

    @Override
    public Optional<AuthToken> lookupToken(String tokenValue) {
        return Optional.ofNullable(activeSessions.get(tokenValue));
    }

    @Override
    public void invalidateToken(String tokenValue) {
        activeSessions.remove(tokenValue);
    }

    @Override
    public void invalidateTokensForUser(int userId, AuthToken.Role role) {
        activeSessions.entrySet().removeIf(entry ->
                entry.getValue().getLinkedUserId() == userId && entry.getValue().getRole() == role);
    }

    @Override
    public void updateTotalFund(long amount) {
        this.grantPoolSize = amount;
    }

    @Override
    public long readTotalFund() {
        return grantPoolSize;
    }

    @Override
    public void updateScoreThreshold(double threshold) {
        this.minimumScore = threshold;
    }

    @Override
    public double readScoreThreshold() {
        return minimumScore;
    }

    @Override
    public boolean isContestFinished() {
        return contestClosedFlag;
    }

    @Override
    public void markContestFinished() {
        this.contestClosedFlag = true;
    }

    @Override
    public void resetContestState() {
        this.contestClosedFlag = false;
        this.selectedWinners = new ArrayList<>();
    }

    @Override
    public List<GrantRequest> readWinningRequests() {
        return new ArrayList<>(selectedWinners);
    }

    @Override
    public void writeWinningRequests(List<GrantRequest> winners) {
        this.selectedWinners = new ArrayList<>(winners);
    }

    @Override
    public int allocateContenderId() {
        return contenderSeq.getAndIncrement();
    }

    @Override
    public int allocateReviewerId() {
        return reviewerSeq.getAndIncrement();
    }

    @Override
    public int allocateRequestId() {
        return requestSeq.getAndIncrement();
    }
}
