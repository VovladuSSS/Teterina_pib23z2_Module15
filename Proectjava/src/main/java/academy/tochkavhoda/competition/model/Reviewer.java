package academy.tochkavhoda.competition.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Expert who evaluates grant requests in specific subject areas.
 */
public class Reviewer {
    private int identifier;
    private String givenName;
    private String familyName;
    private List<String> expertiseAreas = new ArrayList<>();
    private String credentialsLogin;
    private String credentialsSecret;
    private Set<Integer> evaluatedRequestIds = new HashSet<>();

    public Reviewer() {
    }

    public Reviewer(int identifier, String givenName, String familyName, List<String> expertiseAreas,
                    String credentialsLogin, String credentialsSecret) {
        this.identifier = identifier;
        this.givenName = givenName;
        this.familyName = familyName;
        this.expertiseAreas = expertiseAreas != null ? new ArrayList<>(expertiseAreas) : new ArrayList<>();
        this.credentialsLogin = credentialsLogin;
        this.credentialsSecret = credentialsSecret;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public List<String> getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(List<String> expertiseAreas) {
        this.expertiseAreas = expertiseAreas != null ? new ArrayList<>(expertiseAreas) : new ArrayList<>();
    }

    public String getCredentialsLogin() {
        return credentialsLogin;
    }

    public void setCredentialsLogin(String credentialsLogin) {
        this.credentialsLogin = credentialsLogin;
    }

    public String getCredentialsSecret() {
        return credentialsSecret;
    }

    public void setCredentialsSecret(String credentialsSecret) {
        this.credentialsSecret = credentialsSecret;
    }

    public Set<Integer> getEvaluatedRequestIds() {
        return evaluatedRequestIds;
    }

    public void setEvaluatedRequestIds(Set<Integer> evaluatedRequestIds) {
        this.evaluatedRequestIds = evaluatedRequestIds != null ? new HashSet<>(evaluatedRequestIds) : new HashSet<>();
    }

    public void markAsEvaluated(int requestId) {
        this.evaluatedRequestIds.add(requestId);
    }

    public void unmarkEvaluation(int requestId) {
        this.evaluatedRequestIds.remove(requestId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Reviewer reviewer = (Reviewer) other;
        return identifier == reviewer.identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
