package models;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingDates {
    private String checkin;
    private String checkout;
}
