package academy.tochkavhoda.competition.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single application for a grant submitted by a contender.
 */
public class GrantRequest {
    private int identifier;
    private int ownerContenderId;
    private String requestTitle;
    private String requestDescription;
    private List<String> subjectAreas = new ArrayList<>();
    private long requestedSum;
    private Map<Integer, Integer> evaluationScores = new HashMap<>(); // reviewerId -> score 1..5

    public GrantRequest() {
    }

    public GrantRequest(int identifier, int ownerContenderId, String requestTitle, String requestDescription,
                        List<String> subjectAreas, long requestedSum) {
        this.identifier = identifier;
        this.ownerContenderId = ownerContenderId;
        this.requestTitle = requestTitle;
        this.requestDescription = requestDescription;
        this.subjectAreas = subjectAreas != null ? new ArrayList<>(subjectAreas) : new ArrayList<>();
        this.requestedSum = requestedSum;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public int getOwnerContenderId() {
        return ownerContenderId;
    }

    public void setOwnerContenderId(int ownerContenderId) {
        this.ownerContenderId = ownerContenderId;
    }

    public String getRequestTitle() {
        return requestTitle;
    }

    public void setRequestTitle(String requestTitle) {
        this.requestTitle = requestTitle;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public void setRequestDescription(String requestDescription) {
        this.requestDescription = requestDescription;
    }

    public List<String> getSubjectAreas() {
        return subjectAreas;
    }

    public void setSubjectAreas(List<String> subjectAreas) {
        this.subjectAreas = subjectAreas != null ? new ArrayList<>(subjectAreas) : new ArrayList<>();
    }

    public long getRequestedSum() {
        return requestedSum;
    }

    public void setRequestedSum(long requestedSum) {
        this.requestedSum = requestedSum;
    }

    public Map<Integer, Integer> getEvaluationScores() {
        return evaluationScores;
    }

    public void setEvaluationScores(Map<Integer, Integer> evaluationScores) {
        this.evaluationScores = evaluationScores != null ? new HashMap<>(evaluationScores) : new HashMap<>();
    }

    public void putScore(int reviewerId, int score) {
        this.evaluationScores.put(reviewerId, score);
    }

    public void eraseScore(int reviewerId) {
        this.evaluationScores.remove(reviewerId);
    }

    public double computeAverageScore() {
        if (evaluationScores.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (int score : evaluationScores.values()) {
            sum += score;
        }
        return sum / evaluationScores.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        GrantRequest that = (GrantRequest) other;
        return identifier == that.identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
