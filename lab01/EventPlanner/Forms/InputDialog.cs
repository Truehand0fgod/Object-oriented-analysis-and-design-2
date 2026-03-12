using System;
using System.Drawing;
using System.Windows.Forms;

namespace EventPlanner.Forms
{
    public class InputDialog : Form
    {
        private TextBox txtInput;
        private Button btnOk;
        private Button btnCancel;
        private Label lblPrompt;

        public string InputText { get; private set; }

        public InputDialog(string prompt, string title, string defaultValue = "")
        {
            InitializeComponents(prompt, title, defaultValue);
        }

        private void InitializeComponents(string prompt, string title, string defaultValue)
        {
            this.Text = title;
            this.Size = new Size(400, 180);
            this.StartPosition = FormStartPosition.CenterParent;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;

            lblPrompt = new Label
            {
                Text = prompt,
                Location = new Point(12, 20),
                Size = new Size(360, 20)
            };

            txtInput = new TextBox
            {
                Text = defaultValue,
                Location = new Point(12, 45),
                Size = new Size(360, 20)
            };

            btnOk = new Button
            {
                Text = "OK",
                Location = new Point(220, 80),
                Size = new Size(75, 25),
                DialogResult = DialogResult.OK
            };

            btnCancel = new Button
            {
                Text = "Отмена",
                Location = new Point(300, 80),
                Size = new Size(75, 25),
                DialogResult = DialogResult.Cancel
            };

            btnOk.Click += (s, e) => { InputText = txtInput.Text; };

            this.Controls.AddRange(new Control[] { lblPrompt, txtInput, btnOk, btnCancel });
        }
    }
}