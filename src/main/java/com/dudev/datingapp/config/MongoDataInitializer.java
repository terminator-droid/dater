package com.dudev.datingapp.config;

import com.dudev.datingapp.topic.document.ConversationTopic;
import com.dudev.datingapp.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoDataInitializer implements CommandLineRunner {

    private final TopicRepository topicRepository;

    @Override
    public void run(String... args) {
        if (topicRepository.count() > 0) {
            return;
        }
        topicRepository.saveAll(seedTopics());
        log.info("Seeded {} conversation topics", topicRepository.count());
    }

    private List<ConversationTopic> seedTopics() {
        return List.of(
                topic("icebreaker", "О чём ты сегодня думал весь день?",             List.of("мысли", "день")),
                topic("icebreaker", "Что было лучшего в твоей неделе?",              List.of("неделя")),
                topic("icebreaker", "Если бы ты мог поехать куда угодно прямо сейчас, куда бы направился?", List.of("путешествия")),
                topic("icebreaker", "Что тебя последний раз по-настоящему удивило?", List.of("эмоции")),
                topic("icebreaker", "Какую суперспособность ты бы хотел иметь?",     List.of("фантазия")),
                topic("icebreaker", "Чем ты занимаешься, когда не работаешь?",       List.of("хобби")),
                topic("icebreaker", "Что ты обычно делаешь по выходным?",            List.of("выходные")),

                topic("drink", "Что пьёшь сегодня и почему именно это?",             List.of("алкоголь", "выбор")),
                topic("drink", "Есть ли у тебя коктейль, который заказываешь всегда?", List.of("коктейль")),
                topic("drink", "Что из алкоголя попробовал последним — и это тебя удивило?", List.of("открытие")),
                topic("drink", "Вино или крепкое — что сегодня ближе по настроению?", List.of("настроение")),
                topic("drink", "Пробовал что-нибудь из меню этого места раньше?",    List.of("меню", "бар")),

                topic("venue", "Как часто ты бываешь в этом районе?",                List.of("район")),
                topic("venue", "Есть ли у тебя место в городе, куда ты всегда возвращаешься?", List.of("место")),
                topic("venue", "Что тебе нравится в этом баре больше всего?",        List.of("бар", "атмосфера")),
                topic("venue", "Ты обычно сам выбираешь место или идёшь куда позовут?", List.of("выбор")),
                topic("venue", "Есть ли бар или ресторан, который ты хочешь посетить давно, но всё никак?", List.of("список", "мечты")),

                topic("fun_fact", "Какой факт о себе тебя самого до сих пор удивляет?", List.of("факт", "я")),
                topic("fun_fact", "Чем ты занимался 5 лет назад — и изменилось ли это?", List.of("прошлое")),
                topic("fun_fact", "Есть ли у тебя нестандартное хобби?",             List.of("хобби")),
                topic("fun_fact", "Что ты сейчас читаешь или смотришь?",             List.of("книги", "кино")),
                topic("fun_fact", "Какой язык ты хотел бы выучить?",                 List.of("языки")),
                topic("fun_fact", "Есть ли у тебя традиция, которую ты никогда не нарушаешь?", List.of("традиции")),
                topic("fun_fact", "Какую музыку ты слушал сегодня по дороге сюда?",  List.of("музыка")),
                topic("fun_fact", "Если бы ты мог поужинать с любым человеком из истории, кто бы это был?", List.of("история", "фантазия"))
        );
    }

    private ConversationTopic topic(String category, String text, List<String> tags) {
        ConversationTopic t = new ConversationTopic();
        t.setCategory(category);
        t.setText(text);
        t.setTags(tags);
        t.setLocale("ru");
        return t;
    }
}
