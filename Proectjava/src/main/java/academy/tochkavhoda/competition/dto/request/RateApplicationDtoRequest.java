package academy.tochkavhoda.competition.dto.request;

public class RateApplicationDtoRequest {
    private int applicationId;
    private int score;

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
