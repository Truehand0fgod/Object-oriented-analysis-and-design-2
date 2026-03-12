using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;
using EventPlanner.Models;
using EventPlanner.Services;

namespace EventPlanner.Forms
{
    public class MainForm : Form
    {
        private ListBox _listBoxTemplates;
        private FlowLayoutPanel _panelEvents;
        private Button _btnAdd, _btnEdit, _btnCreateEvent, _btnDelete;
        private ITemplateRepository _repository;

        public MainForm()
        {
            _repository = new PrototypeTemplateRepository(); // С паттерном
            // _repository = new TemplateRepository(); // Без паттерна

            InitializeComponent();
            RefreshTemplatesList();
        }

        private void InitializeComponent()
        {
            this.Text = "Планировщик мероприятий";
            this.Size = new Size(1000, 600);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.BackColor = Color.FromArgb(240, 240, 245);

            var leftPanel = CreateLeftPanel();
            var rightPanel = CreateRightPanel();

            this.Controls.Add(leftPanel);
            this.Controls.Add(rightPanel);
        }

        private Panel CreateLeftPanel()
        {
            var panel = new Panel
            {
                Location = new Point(10, 10),
                Size = new Size(300, 540),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.White
            };

            panel.Controls.Add(new Label
            {
                Text = "Шаблоны мероприятий",
                Location = new Point(10, 10),
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                AutoSize = true
            });

            _listBoxTemplates = new ListBox
            {
                Location = new Point(10, 40),
                Size = new Size(280, 350),
                DrawMode = DrawMode.OwnerDrawFixed,
                ItemHeight = 50
            };
            _listBoxTemplates.DrawItem += ListBoxTemplates_DrawItem;
            _listBoxTemplates.SelectedIndexChanged += (s, e) => UpdateButtonsState();
            panel.Controls.Add(_listBoxTemplates);

            _btnAdd = CreateButton("Новый шаблон", new Point(10, 400), Color.FromArgb(46, 204, 113));
            _btnAdd.Click += BtnAdd_Click;

            _btnEdit = CreateButton("Редактировать", new Point(10, 430), Color.FromArgb(52, 152, 219));
            _btnEdit.Click += BtnEdit_Click;

            _btnDelete = CreateButton("Удалить", new Point(155, 430), Color.FromArgb(231, 76, 60));
            _btnDelete.Click += BtnDelete_Click;

            _btnCreateEvent = CreateButton("Создать мероприятие", new Point(10, 470), Color.FromArgb(155, 89, 182));
            _btnCreateEvent.Size = new Size(280, 35);
            _btnCreateEvent.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            _btnCreateEvent.Click += BtnCreateEvent_Click;

            panel.Controls.AddRange(new Control[] {
                _btnAdd, _btnEdit, _btnDelete, _btnCreateEvent
            });

            return panel;
        }

        private Panel CreateRightPanel()
        {
            var panel = new Panel
            {
                Location = new Point(320, 10),
                Size = new Size(660, 540),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.White,
                AutoScroll = true
            };

            panel.Controls.Add(new Label
            {
                Text = "Созданные мероприятия",
                Location = new Point(10, 10),
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                AutoSize = true
            });

            _panelEvents = new FlowLayoutPanel
            {
                Location = new Point(10, 40),
                Size = new Size(640, 490),
                AutoScroll = true,
                BackColor = Color.FromArgb(250, 250, 255)
            };

            panel.Controls.Add(_panelEvents);
            return panel;
        }

        private Button CreateButton(string text, Point location, Color color)
        {
            return new Button
            {
                Text = text,
                Location = location,
                Size = new Size(140, 25),
                BackColor = color,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9, FontStyle.Bold)
            };
        }

        private void ListBoxTemplates_DrawItem(object sender, DrawItemEventArgs e)
        {
            if (e.Index < 0) return;

            e.DrawBackground();

            var template = (EventTemplate)_listBoxTemplates.Items[e.Index];

            try
            {
                using (var brush = new SolidBrush(ColorTranslator.FromHtml(template.ColorCode)))
                {
                    e.Graphics.FillRectangle(brush, e.Bounds.X + 5, e.Bounds.Y + 5, 40, 40);
                }
            }
            catch
            {
                using (var brush = new SolidBrush(Color.Gray))
                {
                    e.Graphics.FillRectangle(brush, e.Bounds.X + 5, e.Bounds.Y + 5, 40, 40);
                }
            }

            e.Graphics.DrawRectangle(Pens.Gray, e.Bounds.X + 5, e.Bounds.Y + 5, 40, 40);

            using (var font = new Font("Segoe UI", 10, FontStyle.Bold))
            {
                e.Graphics.DrawString(template.Name, font, Brushes.Black,
                    e.Bounds.X + 55, e.Bounds.Y + 8);
            }

            using (var font = new Font("Segoe UI", 8))
            {
                string info = $"{template.ExpectedGuests} гостей | {template.Duration.Hours} ч | {template.Budget}т.р.";
                e.Graphics.DrawString(info, font, Brushes.DimGray,
                    e.Bounds.X + 55, e.Bounds.Y + 28);
            }

            e.DrawFocusRectangle();
        }

        private void RefreshTemplatesList()
        {
            _listBoxTemplates.Items.Clear();
            foreach (var template in _repository.GetAll())
            {
                _listBoxTemplates.Items.Add(template);
            }
            UpdateButtonsState();
        }

        private void UpdateButtonsState()
        {
            bool hasSelection = _listBoxTemplates.SelectedItem != null;
            _btnEdit.Enabled = hasSelection;
            _btnCreateEvent.Enabled = hasSelection;
            _btnDelete.Enabled = hasSelection;
        }

        private void BtnAdd_Click(object sender, EventArgs e)
        {
            using (var editor = new TemplateEditorForm())
            {
                if (editor.ShowDialog() == DialogResult.OK)
                {
                    _repository.Add(editor.Template);
                    RefreshTemplatesList();
                }
            }
        }

        private void BtnEdit_Click(object sender, EventArgs e)
        {
            if (!(_listBoxTemplates.SelectedItem is EventTemplate selected)) return;

            using (var editor = new TemplateEditorForm(selected))
            {
                if (editor.ShowDialog() == DialogResult.OK)
                {
                    _repository.Update(selected, editor.Template);
                    RefreshTemplatesList();
                }
            }
        }

        private void BtnCreateEvent_Click(object sender, EventArgs e)
        {
            if (!(_listBoxTemplates.SelectedItem is EventTemplate selected)) return;

            using (var dialog = new InputDialog(
                "Введите название мероприятия:",
                "Создание мероприятия",
                $"{selected.Name} на {DateTime.Now:dd.MM}"))
            {
                if (dialog.ShowDialog() == DialogResult.OK && !string.IsNullOrWhiteSpace(dialog.InputText))
                {
                    var newEvent = _repository.CreateEventFromTemplate(selected, dialog.InputText);
                    AddEventCard(newEvent);
                }
            }
        }

        private void AddEventCard(EventTemplate eventTemplate)
        {
            var card = CreateEventCard(eventTemplate);
            _panelEvents.Controls.Add(card);
            _panelEvents.ScrollControlIntoView(card);
        }

        private void UpdateEventCard(Panel card, EventTemplate updatedEvent)
        {
            foreach (Control control in card.Controls)
            {
                if (control is Label lbl)
                {
                    if (lbl.Name == "lblEventName")
                    {
                        lbl.Text = updatedEvent.Name;
                    }
                    else if (lbl.Name == "lblEventDetails")
                    {
                        lbl.Text = $"{updatedEvent.ExpectedGuests} гостей | " +
                                  $"{updatedEvent.Duration.Hours} ч {updatedEvent.Duration.Minutes} мин | " +
                                  $"{updatedEvent.Budget}т.р.";
                    }
                }
                else if (control is Panel pnl && pnl.Name == "colorBar")
                {
                    try { pnl.BackColor = ColorTranslator.FromHtml(updatedEvent.ColorCode); }
                    catch { pnl.BackColor = Color.Gray; }
                }
            }

            card.Tag = updatedEvent;
        }

        private Panel CreateEventCard(EventTemplate template)
        {
            var card = new Panel
            {
                Size = new Size(200, 230),
                Margin = new Padding(10),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.White,
                Tag = template
            };

            var colorBar = new Panel
            {
                Name = "colorBar",
                Location = new Point(0, 0),
                Size = new Size(200, 15)
            };
            try { colorBar.BackColor = ColorTranslator.FromHtml(template.ColorCode); }
            catch { colorBar.BackColor = Color.Gray; }

            var lblName = new Label
            {
                Name = "lblEventName",
                Text = template.Name,
                Location = new Point(5, 20),
                Size = new Size(190, 30),
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                TextAlign = ContentAlignment.MiddleCenter
            };

            var lblDetails = new Label
            {
                Name = "lblEventDetails",
                Text = $"{template.ExpectedGuests} гостей\n" +
                      $"{template.Duration.Hours} ч {template.Duration.Minutes} мин\n" +
                      $"{template.Budget}т.р.",
                Location = new Point(5, 55),
                Size = new Size(190, 70),
                Font = new Font("Segoe UI", 9),
                TextAlign = ContentAlignment.MiddleCenter
            };

            var btnDetails = new Button
            {
                Text = "Подробнее",
                Location = new Point(10, 135),
                Size = new Size(180, 25),
                BackColor = Color.FromArgb(52, 152, 219),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };

            btnDetails.Click += (s, e) => {
                string items = template.RequiredItems.Count > 0
                    ? string.Join(", ", template.RequiredItems)
                    : "не указаны";

                MessageBox.Show(
                    $"Мероприятие: {template.Name}\n" +
                    $"Тема: {template.Theme}\n" +
                    $"Гостей: {template.ExpectedGuests}\n" +
                    $"Длительность: {template.Duration.Hours} ч {template.Duration.Minutes} мин\n" +
                    $"Бюджет: ${template.Budget}\n" +
                    $"Необходимо: {items}",
                    "Детали мероприятия",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information
                );
            };

            var btnEdit = new Button
            {
                Text = "Редактировать",
                Location = new Point(10, 165),
                Size = new Size(180, 25),
                BackColor = Color.FromArgb(46, 204, 113),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };

            btnEdit.Click += (s, e) => {
                using (var editForm = new EditEventForm(template))
                {
                    if (editForm.ShowDialog() == DialogResult.OK)
                    {
                        UpdateEventCard(card, editForm.Event);
                        template = editForm.Event;
                    }
                }
            };

            card.Controls.AddRange(new Control[] {
        colorBar, lblName, lblDetails, btnDetails, btnEdit
    });

            return card;
        }

        private void BtnDelete_Click(object sender, EventArgs e)
        {
            if (!(_listBoxTemplates.SelectedItem is EventTemplate selected)) return;

            var result = MessageBox.Show($"Удалить шаблон '{selected.Name}'?",
                "Подтверждение", MessageBoxButtons.YesNo, MessageBoxIcon.Question);

            if (result == DialogResult.Yes)
            {
                _repository.Delete(selected);
                RefreshTemplatesList();
            }
        }
    }
}