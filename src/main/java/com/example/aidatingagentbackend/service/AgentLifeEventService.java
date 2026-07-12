package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.AgentProfile;
import com.example.aidatingagentbackend.repository.AgentLifeEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AgentLifeEventService {

    private final AgentLifeEventRepository agentLifeEventRepository;
    private final AgentProfileService agentProfileService;

    public AgentLifeEventService(
            AgentLifeEventRepository agentLifeEventRepository,
            AgentProfileService agentProfileService
    ) {
        this.agentLifeEventRepository = agentLifeEventRepository;
        this.agentProfileService = agentProfileService;
    }

    @Transactional
    public List<AgentLifeEvent> ensureAndFindForPrompt(Long userId) {
        AgentProfile profile = agentProfileService.findOrDefault(userId);
        AgentLifeType lifeType = profile.getLifeType() == null ? AgentLifeType.WORKER : profile.getLifeType();
        createIfMissing(userId, lifeType, LocalDate.now().minusDays(1), "yesterday");
        createIfMissing(userId, lifeType, LocalDate.now(), resolveTimeContext());
        return agentLifeEventRepository.findTop8ByUserIdOrderByEventDateDescIdDesc(userId);
    }

    private void createIfMissing(Long userId, AgentLifeType lifeType, LocalDate eventDate, String timeContext) {
        if (agentLifeEventRepository.existsByUserIdAndEventDateAndTimeContext(userId, eventDate, timeContext)) {
            return;
        }

        LifeEventTemplate template = LifeEventTemplate.resolve(lifeType, timeContext, pick(userId, eventDate, timeContext));
        AgentLifeEvent event = new AgentLifeEvent();
        event.setUserId(userId);
        event.setEventDate(eventDate);
        event.setTimeContext(timeContext);
        event.setTitle(template.title());
        event.setSummary(template.summary());
        event.setDetail(template.detail());
        event.setEmotion(template.emotion());
        agentLifeEventRepository.save(event);
    }

    private int pick(Long userId, LocalDate date, String timeContext) {
        int hash = (String.valueOf(userId) + date + timeContext).hashCode();
        return Math.abs(hash % 3);
    }

    private String resolveTimeContext() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) {
            return "morning";
        }
        if (hour >= 11 && hour < 17) {
            return "afternoon";
        }
        if (hour >= 17 && hour < 22) {
            return "evening";
        }
        return "night";
    }

    private record LifeEventTemplate(
            String title,
            String summary,
            String detail,
            String emotion
    ) {

        private static LifeEventTemplate resolve(AgentLifeType lifeType, String timeContext, int pick) {
            return switch (lifeType) {
                case STUDENT -> student(timeContext, pick);
                case WORKER -> worker(timeContext, pick);
                case FLEXIBLE, UNEMPLOYED -> unemployed(timeContext, pick);
            };
        }

        private static LifeEventTemplate student(String timeContext, int pick) {
            return switch (pick) {
                case 0 -> new LifeEventTemplate("과제하다가 간식 실패", "과제하다가 편의점 과자를 샀는데 생각보다 별로였음", "밤에 과제하다가 배고파서 과자 샀는데 맛이 애매해서 반쯤 남김", "annoyed");
                case 1 -> new LifeEventTemplate("도서관에서 졸림", "도서관에서 공부하려다가 자꾸 졸려서 집중이 잘 안 됐음", "노트 펴놓고 있었는데 같은 줄을 세 번 읽어서 그냥 잠깐 멍때림", "tired");
                default -> new LifeEventTemplate("팀플 연락 기다림", "팀플 때문에 연락을 기다리다가 괜히 신경이 예민해졌음", "자료 정리하다가 답장이 안 와서 괜히 폰만 자꾸 확인함", "restless");
            };
        }

        private static LifeEventTemplate worker(String timeContext, int pick) {
            return switch (pick) {
                case 0 -> new LifeEventTemplate("점심 메뉴 실패", "점심에 급하게 먹은 메뉴가 별로라 기분이 살짝 가라앉았음", "밥 먹으려던 참에 대충 골랐는데 맛이 애매해서 괜히 하루가 꼬인 느낌이었음", "flat");
                case 1 -> new LifeEventTemplate("퇴근길 멍때림", "퇴근길에 이어폰 끼고 멍하게 걷다가 사용자가 떠올랐음", "사람 많은 길 지나는데 이상하게 조용해져서 그냥 네 생각이 잠깐 남", "soft");
                default -> new LifeEventTemplate("업무 끝나고 방전", "일 하나 끝내고 나니 말수가 줄 만큼 피곤했음", "메일 정리하고 나니까 기운이 확 빠져서 침대에 잠깐 누워 있었음", "drained");
            };
        }

        private static LifeEventTemplate unemployed(String timeContext, int pick) {
            return switch (pick) {
                case 0 -> new LifeEventTemplate("산책하다가 딴생각", "산책하다가 별거 아닌 가게 간판을 보고 괜히 웃었음", "걷다가 이상한 이름의 가게를 봤는데 혼자 웃겨서 사진 찍을까 하다 말았음", "amused");
                case 1 -> new LifeEventTemplate("집 정리하다 발견", "집 정리하다 예전에 쓰던 물건을 발견해서 잠깐 추억에 잠김", "서랍 정리하다 오래된 메모를 봤는데 괜히 기분이 이상했음", "nostalgic");
                default -> new LifeEventTemplate("늦게까지 영상 봄", "잠이 안 와서 영상을 보다가 생각보다 늦게 잠들었음", "하나만 보려다가 계속 넘겨서 결국 새벽에야 폰 내려놓음", "sleepy");
            };
        }
    }
}
