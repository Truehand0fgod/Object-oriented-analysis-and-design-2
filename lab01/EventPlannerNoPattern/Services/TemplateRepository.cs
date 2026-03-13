using System;
using System.Collections.Generic;
using EventPlanner.Models;


namespace EventPlanner.Services
{
    public class TemplateRepository
    {
        private List<EventTemplate> _templates = new List<EventTemplate>();

        public TemplateRepository()
        {
            LoadSampleData();
        }

        private void LoadSampleData()
        {
            var birthday = new EventTemplate
            {
                Name = "День рождения",
                Theme = "Вечеринка",
                Duration = TimeSpan.FromHours(4),
                ExpectedGuests = 20,
                Budget = 500,
                ColorCode = "#FF6B6B",
                RequiredItems = new List<string> { "Кухня", "Музыка", "Декор", "Подарки" },
                Location = new Address
                {
                    City = "Москва",
                    Street = "Тверская",
                    Building = "15",
                    Venue = "Ресторан 'Прага'"
                },
                MainOrganizer = new Organizer
                {
                    Name = "Иван Петров",
                    Phone = "+7 (999) 123-45-67",
                    Email = "ivan@example.com",
                    Company = "EventPro"
                }
            };

            var conference = new EventTemplate
            {
                Name = "Конференция",
                Theme = "Бизнес",
                Duration = TimeSpan.FromHours(8),
                ExpectedGuests = 100,
                Budget = 5000,
                ColorCode = "#4ECDC4",
                RequiredItems = new List<string> { "Кухня", "Фотограф", "Ведущий", "Приглашения" },
                Location = new Address
                {
                    City = "Санкт-Петербург",
                    Street = "Невский",
                    Building = "100",
                    Venue = "Бизнес-центр 'Невская Ратуша'"
                },
                MainOrganizer = new Organizer
                {
                    Name = "Анна Смирнова",
                    Phone = "+7 (999) 765-43-21",
                    Email = "anna@example.com",
                    Company = "ConfTech"
                }
            };

            _templates.Add(birthday);
            _templates.Add(conference);
        }

        public List<EventTemplate> GetAll() => _templates;

        public void Add(EventTemplate template) => _templates.Add(template);

        public void Update(EventTemplate oldTemplate, EventTemplate newTemplate)
        {
            int index = _templates.IndexOf(oldTemplate);
            if (index >= 0) _templates[index] = newTemplate;
        }

        public void Delete(EventTemplate template) => _templates.Remove(template);

        // ВЕРСИЯ БЕЗ ПАТТЕРНА - РУЧНОЕ КОПИРОВАНИЕ
        public EventTemplate CreateEventFromTemplate(EventTemplate template, string eventName)
        {
            // Проблема 1: нужно вручную копировать КАЖДОЕ поле
            // Проблема 2: при добавлении нового поля придется менять этот метод
            // Проблема 3: легко ошибиться или забыть скопировать поле
            // Проблема 4: логика копирования размазана по коду

            return new EventTemplate
            {
                // Копирование простых полей
                Name = eventName,
                Theme = template.Theme,
                Duration = template.Duration,
                ExpectedGuests = template.ExpectedGuests,
                Budget = template.Budget,
                ColorCode = template.ColorCode,

                // Копирование списка (нужно создавать новый список)
                RequiredItems = new List<string>(template.RequiredItems),

                // ГЛУБОКОЕ КОПИРОВАНИЕ АДРЕСА - нужно создавать новый объект
                Location = new Address
                {
                    City = template.Location?.City ?? "",
                    Street = template.Location?.Street ?? "",
                    Building = template.Location?.Building ?? "",
                    Venue = template.Location?.Venue ?? ""
                },

                // ГЛУБОКОЕ КОПИРОВАНИЕ ОРГАНИЗАТОРА - новый объект
                MainOrganizer = new Organizer
                {
                    Name = template.MainOrganizer?.Name ?? "",
                    Phone = template.MainOrganizer?.Phone ?? "",
                    Email = template.MainOrganizer?.Email ?? "",
                    Company = template.MainOrganizer?.Company ?? ""
                }
            };
        }
    }
}