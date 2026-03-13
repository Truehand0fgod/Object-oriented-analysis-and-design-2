using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;
using EventPlanner.Models;

namespace EventPlanner.Forms
{
    public class TemplateEditorForm : Form
    {
        private const int FORM_WIDTH = 500;
        private const int FORM_HEIGHT = 750; // Увеличили высоту
        private const int CONTROL_OFFSET = 130;

        private TextBox _txtName;
        private TextBox _txtTheme;
        private TextBox _txtBudget;
        private NumericUpDown _nudGuests;
        private NumericUpDown _nudHours;
        private NumericUpDown _nudMinutes;
        private CheckedListBox _clbItems;
        private Panel _colorPreview;
        private ColorDialog _colorDialog;
        private TextBox _txtNewItem;

        private TextBox _txtCity;
        private TextBox _txtStreet;
        private TextBox _txtBuilding;
        private TextBox _txtVenue;

        private TextBox _txtOrgName;
        private TextBox _txtCompany;
        private TextBox _txtPhone;
        private TextBox _txtEmail;

        private Button _btnSave;
        private Button _btnCancel;

        public EventTemplate Template { get; private set; }
        public string SelectedColor { get; private set; }

        public TemplateEditorForm(EventTemplate template = null)
        {
            InitializeComponent();

            Template = template != null ? new EventTemplate(template) : new EventTemplate();
            SelectedColor = Template.ColorCode ?? "#FF6B6B";

            LoadTemplateData();
        }

        private void InitializeComponent()
        {
            this.Text = "Редактор шаблона";
            this.Size = new Size(FORM_WIDTH, FORM_HEIGHT);
            this.StartPosition = FormStartPosition.CenterParent;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;

            int currentY = 20;

            this.Controls.Add(CreateLabel("Название:", 20, currentY));
            _txtName = CreateTextBox(CONTROL_OFFSET, currentY, 300);
            this.Controls.Add(_txtName);
            currentY += 30;

            this.Controls.Add(CreateLabel("Тема:", 20, currentY));
            _txtTheme = CreateTextBox(CONTROL_OFFSET, currentY, 300);
            this.Controls.Add(_txtTheme);
            currentY += 30;

            this.Controls.Add(CreateLabel("Длительность:", 20, currentY));

            _nudHours = CreateNumericUpDown(CONTROL_OFFSET, currentY, 60, 0, 24);
            _nudHours.Value = 2;
            this.Controls.Add(_nudHours);
            this.Controls.Add(CreateLabel("ч", CONTROL_OFFSET + 65, currentY, 20));

            _nudMinutes = CreateNumericUpDown(CONTROL_OFFSET + 90, currentY, 60, 0, 59);
            _nudMinutes.Value = 0;
            this.Controls.Add(_nudMinutes);
            this.Controls.Add(CreateLabel("мин", CONTROL_OFFSET + 155, currentY, 40));
            currentY += 30;

            this.Controls.Add(CreateLabel("Гостей:", 20, currentY));
            _nudGuests = CreateNumericUpDown(CONTROL_OFFSET, currentY, 100, 1, 1000);
            _nudGuests.Value = 20;
            this.Controls.Add(_nudGuests);
            currentY += 30;

            this.Controls.Add(CreateLabel("Бюджет (тыс. руб.):", 20, currentY));
            _txtBudget = CreateTextBox(CONTROL_OFFSET, currentY, 100);
            _txtBudget.Text = "500";
            this.Controls.Add(_txtBudget);
            currentY += 30;

            this.Controls.Add(CreateLabel("Цвет:", 20, currentY));
            _colorPreview = new Panel
            {
                Location = new Point(CONTROL_OFFSET, currentY),
                Size = new Size(50, 25),
                BackColor = Color.FromArgb(255, 107, 107),
                BorderStyle = BorderStyle.FixedSingle,
                Cursor = Cursors.Hand
            };
            _colorPreview.Click += ColorPreview_Click;
            _colorDialog = new ColorDialog();
            this.Controls.Add(_colorPreview);
            currentY += 35;

            this.Controls.Add(CreateLabel("Необходимо:", 20, currentY));
            _clbItems = new CheckedListBox
            {
                Location = new Point(CONTROL_OFFSET, currentY),
                Width = 300,
                Height = 120,
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.White
            };

            string[] defaultItems = {
                "Кухня", "Музыка", "Декор", "Фотограф",
                "Ведущий", "Транспорт", "Подарки", "Приглашения"
            };
            _clbItems.Items.AddRange(defaultItems);
            this.Controls.Add(_clbItems);
            currentY += 130;

            _txtNewItem = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            this.Controls.Add(_txtNewItem);

            var btnAddItem = CreateButton("Добавить", CONTROL_OFFSET + 210, currentY, 90, 25);
            btnAddItem.BackColor = Color.FromArgb(76, 175, 80);
            btnAddItem.Click += (s, e) => AddNewItem();
            this.Controls.Add(btnAddItem);
            currentY += 35;

            this.Controls.Add(CreateLabel("ГОРОД:", 20, currentY));
            _txtCity = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtCity.Name = "txtCity";
            this.Controls.Add(_txtCity);
            currentY += 30;

            this.Controls.Add(CreateLabel("Улица:", 20, currentY));
            _txtStreet = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtStreet.Name = "txtStreet";
            this.Controls.Add(_txtStreet);
            currentY += 30;

            this.Controls.Add(CreateLabel("Дом:", 20, currentY));
            _txtBuilding = CreateTextBox(CONTROL_OFFSET, currentY, 80);
            _txtBuilding.Name = "txtBuilding";
            this.Controls.Add(_txtBuilding);
            currentY += 30;

            this.Controls.Add(CreateLabel("Место:", 20, currentY));
            _txtVenue = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtVenue.Name = "txtVenue";
            this.Controls.Add(_txtVenue);
            currentY += 35;

            this.Controls.Add(CreateLabel("ОРГАНИЗАТОР:", 20, currentY));
            _txtOrgName = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtOrgName.Name = "txtOrgName";
            this.Controls.Add(_txtOrgName);
            currentY += 30;

            this.Controls.Add(CreateLabel("Компания:", 20, currentY));
            _txtCompany = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtCompany.Name = "txtCompany";
            this.Controls.Add(_txtCompany);
            currentY += 30;

            this.Controls.Add(CreateLabel("Телефон:", 20, currentY));
            _txtPhone = CreateTextBox(CONTROL_OFFSET, currentY, 150);
            _txtPhone.Name = "txtPhone";
            this.Controls.Add(_txtPhone);
            currentY += 30;

            this.Controls.Add(CreateLabel("Email:", 20, currentY));
            _txtEmail = CreateTextBox(CONTROL_OFFSET, currentY, 200);
            _txtEmail.Name = "txtEmail";
            this.Controls.Add(_txtEmail);
            currentY += 35;

            _btnSave = CreateButton("Сохранить", 130, currentY, 150, 40);
            _btnSave.BackColor = Color.FromArgb(52, 152, 219);
            _btnSave.Click += BtnSave_Click;
            this.Controls.Add(_btnSave);

            _btnCancel = CreateButton("Отмена", 300, currentY, 130, 40);
            _btnCancel.BackColor = Color.FromArgb(231, 76, 60);
            _btnCancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            this.Controls.Add(_btnCancel);
        }

        private Label CreateLabel(string text, int x, int y, int width = 100)
        {
            return new Label { Text = text, Location = new Point(x, y + 3), Width = width };
        }

        private TextBox CreateTextBox(int x, int y, int width)
        {
            return new TextBox { Location = new Point(x, y), Width = width };
        }

        private NumericUpDown CreateNumericUpDown(int x, int y, int width, decimal min, decimal max)
        {
            return new NumericUpDown
            {
                Location = new Point(x, y),
                Width = width,
                Minimum = min,
                Maximum = max,
                DecimalPlaces = 0
            };
        }

        private Button CreateButton(string text, int x, int y, int width, int height)
        {
            return new Button
            {
                Text = text,
                Location = new Point(x, y),
                Size = new Size(width, height),
                FlatStyle = FlatStyle.Flat,
                ForeColor = Color.White
            };
        }

        private void AddNewItem()
        {
            if (!string.IsNullOrWhiteSpace(_txtNewItem.Text) &&
                !_clbItems.Items.Contains(_txtNewItem.Text))
            {
                _clbItems.Items.Add(_txtNewItem.Text);
                _txtNewItem.Clear();
            }
        }

        private void ColorPreview_Click(object sender, EventArgs e)
        {
            _colorDialog.Color = _colorPreview.BackColor;
            if (_colorDialog.ShowDialog() == DialogResult.OK)
            {
                _colorPreview.BackColor = _colorDialog.Color;
                SelectedColor = ColorTranslator.ToHtml(_colorDialog.Color);
            }
        }

        private void LoadTemplateData()
        {
            if (Template == null) return;

            _txtName.Text = Template.Name ?? "";
            _txtTheme.Text = Template.Theme ?? "";
            _nudHours.Value = Template.Duration.Hours;
            _nudMinutes.Value = Template.Duration.Minutes;
            _nudGuests.Value = Template.ExpectedGuests > 0 ? Template.ExpectedGuests : 20;
            _txtBudget.Text = Template.Budget.ToString("F2");

            if (!string.IsNullOrEmpty(Template.ColorCode))
            {
                try
                {
                    _colorPreview.BackColor = ColorTranslator.FromHtml(Template.ColorCode);
                }
                catch { }
            }

            if (Template.RequiredItems != null)
            {
                for (int i = 0; i < _clbItems.Items.Count; i++)
                {
                    string item = _clbItems.Items[i].ToString();
                    if (Template.RequiredItems.Contains(item))
                        _clbItems.SetItemChecked(i, true);
                }
            }

            if (Template.Location != null)
            {
                _txtCity.Text = Template.Location.City ?? "";
                _txtStreet.Text = Template.Location.Street ?? "";
                _txtBuilding.Text = Template.Location.Building ?? "";
                _txtVenue.Text = Template.Location.Venue ?? "";
            }

            if (Template.MainOrganizer != null)
            {
                _txtOrgName.Text = Template.MainOrganizer.Name ?? "";
                _txtCompany.Text = Template.MainOrganizer.Company ?? "";
                _txtPhone.Text = Template.MainOrganizer.Phone ?? "";
                _txtEmail.Text = Template.MainOrganizer.Email ?? "";
            }
        }

        private void BtnSave_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(_txtName.Text))
            {
                MessageBox.Show("Введите название мероприятия", "Ошибка",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            Template.Name = _txtName.Text;
            Template.Theme = _txtTheme.Text;
            Template.Duration = new TimeSpan((int)_nudHours.Value, (int)_nudMinutes.Value, 0);
            Template.ExpectedGuests = (int)_nudGuests.Value;

            if (decimal.TryParse(_txtBudget.Text, out decimal budget))
                Template.Budget = budget;

            Template.ColorCode = SelectedColor;

            Template.RequiredItems.Clear();
            foreach (var item in _clbItems.CheckedItems)
            {
                Template.RequiredItems.Add(item.ToString());
            }

            if (Template.Location == null)
                Template.Location = new Address();

            Template.Location.City = _txtCity.Text;
            Template.Location.Street = _txtStreet.Text;
            Template.Location.Building = _txtBuilding.Text;
            Template.Location.Venue = _txtVenue.Text;

            if (Template.MainOrganizer == null)
                Template.MainOrganizer = new Organizer();

            Template.MainOrganizer.Name = _txtOrgName.Text;
            Template.MainOrganizer.Company = _txtCompany.Text;
            Template.MainOrganizer.Phone = _txtPhone.Text;
            Template.MainOrganizer.Email = _txtEmail.Text;

            DialogResult = DialogResult.OK;
        }
    }
}