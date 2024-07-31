package UniFest.domain.announcement.service;

import UniFest.domain.announcement.entity.Announcement;
import UniFest.domain.announcement.repository.AnnouncementRepository;
import UniFest.domain.booth.entity.Booth;
import UniFest.domain.booth.repository.BoothRepository;
import UniFest.dto.request.announcement.AddAnnouncementRequest;
import UniFest.dto.request.announcement.FestivalInterestRequest;
import UniFest.exception.announcement.FcmFailException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.TopicManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final BoothRepository boothRepository;
    private final AnnouncementRepository announcementRepository;

    private List<String> wrapAsRegistrationTokens(FestivalInterestRequest festivalInterestRequest) {
        return Arrays.asList(festivalInterestRequest.getFcmToken());
    }

    public void addFestivalInterest(Long FestivalId, FestivalInterestRequest festivalInterestRequest) {
        List<String> registrationTokens = wrapAsRegistrationTokens(festivalInterestRequest);
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(registrationTokens, String.valueOf(FestivalId));
            if (response.getSuccessCount() != 1) {
                throw new FcmFailException();
            }
        } catch (FirebaseMessagingException e) {
            throw new FcmFailException();
        }
    }

    public void deleteFestivalInterest(Long FestivalId, FestivalInterestRequest festivalInterestRequest) {
        List<String> registrationTokens = wrapAsRegistrationTokens(festivalInterestRequest);
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(registrationTokens, String.valueOf(FestivalId));
            if (response.getSuccessCount() != 1) {
                throw new FcmFailException();
            }
        } catch (FirebaseMessagingException e) {
            throw new FcmFailException();
        }
    }

    public Long addAnnouncement(Long boothId, AddAnnouncementRequest addAnnouncementRequest) {
        Booth booth = boothRepository.getReferenceById(boothId);
        String topic = String.valueOf(booth.getFestival().getId());

        Message message = Message.builder()
                .putData("boothId", String.valueOf(boothId))
                .putData("boothName", booth.getName())
                .putData("msgBody", addAnnouncementRequest.getMsgBody())
                .setTopic(topic)
                .build();

        Announcement announcement = Announcement.builder()
                .msgBody(addAnnouncementRequest.getMsgBody())
                .booth(booth)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
            announcement.setIsSent(true);
        } catch (FirebaseMessagingException e) {
            announcement.setIsSent(false);
            announcement.setErrorMessage(e.getMessage());
            throw new FcmFailException();
        } finally {
            announcementRepository.save(announcement);
        }

        return announcement.getId();
    }
}