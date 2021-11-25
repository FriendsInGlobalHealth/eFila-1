package org.celllife.idart.gui.prescription;

import model.manager.DrugManager;
import org.apache.log4j.Logger;
import org.celllife.idart.commonobjects.CommonObjects;
import org.celllife.idart.database.hibernate.Drug;
import org.celllife.idart.database.hibernate.Form;
import org.celllife.idart.database.hibernate.PrescribedDrugs;
import org.celllife.idart.database.hibernate.RegimenDrugs;
import org.celllife.idart.gui.platform.GenericOthersGui;
import org.celllife.idart.gui.search.Search;
import org.celllife.idart.gui.utils.ResourceUtils;
import org.celllife.idart.gui.utils.iDartColor;
import org.celllife.idart.gui.utils.iDartFont;
import org.celllife.idart.gui.utils.iDartImage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.hibernate.Session;

public class PrescriptionObjectAssociated extends GenericOthersGui {

    private Group grpDrugBarcode;

    private Label lblBarcode;

    private Label lblTake;

    private Text txtTake;

    private Text txtQnty;

    private Label lblDescription;

    private Label lblTakePeriod;

    private Label lblTimes;

    private TableItem tableItem;

    private Drug newDrug;

    private Button drugBarCodeSearch;

    private Text txtDrugName;

    private Group grpDrugInformation;

    private Label lblDrugName;

    private Button btnAddDrug;

    private Button btnClear;

    private Button btnCancel;

    private boolean isRegimen = false;

    private Text txtTimes;

    private CCombo cmbPeriodoToma;

    private String regimeTerapeutico;

    /**
     * Constructor
     *
     * @param hSession  Session
     * @param ti        TableItem
     * @param isRegimen boolean
     * @param parent    Shell
     */
    public PrescriptionObjectAssociated(Session hSession, TableItem ti,
                              boolean isRegimen,String regimeTerapeutico, Shell parent) {
        super(parent, hSession);
        this.isRegimen = isRegimen;
        activate();
        this.tableItem = ti;
        this.regimeTerapeutico = regimeTerapeutico;
        tableItem = ti;
        // should open immediately???
        cmdAssossiationSearchWidgetSelected();
    }

    /**
     * This method initializes getShell()
     */
    @Override
    protected void createShell() {
        String shellTxt = isRegimen ? "Adicionar Medicamento a este Grupo"
                : "Adicionar Medicamento";
        Rectangle bounds = new Rectangle(300, 200, 500, 430);
        buildShell(shellTxt, bounds);
        getShell().addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event e) {
                cmdCancelWidgetSelected();
            }
        });
        createGrpDrugBarcode();
        createGrpDrugInformation();
    }

    /**
     * This method initializes compHeader
     */
    @Override
    protected void createCompHeader() {
        String txt = (isRegimen ? "Adicionar Medicamento a este Grupo"
                : "Adicionar Medicamento");
        iDartImage icoImage = iDartImage.PRESCRIPTIONADDDRUG;
        buildCompHeader(txt, icoImage);
        lblHeader.setSize(lblHeader.getBounds().width + 100, lblHeader
                .getBounds().height);
        lblBg.setSize(lblBg.getBounds().width + 100, lblBg.getBounds().height);
    }

    /*
     * This method initializes grpDrugBarcode
     */
    private void createGrpDrugBarcode() {

        grpDrugBarcode = new Group(getShell(), SWT.NONE);
        grpDrugBarcode.setBounds(new org.eclipse.swt.graphics.Rectangle(45,
                120, 410, 45));

        lblBarcode = new Label(grpDrugBarcode, SWT.NONE);
        lblBarcode.setBounds(new org.eclipse.swt.graphics.Rectangle(15, 15,
                250, 20));
        lblBarcode.setText("Procurar o medicamento a adicionar:");
        lblBarcode.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        drugBarCodeSearch = new Button(grpDrugBarcode, SWT.NONE);
        drugBarCodeSearch.setBounds(new org.eclipse.swt.graphics.Rectangle(280,
                12, 120, 26));
        drugBarCodeSearch.setText("Procurar");
        drugBarCodeSearch.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        drugBarCodeSearch.setEnabled(false);
        drugBarCodeSearch
                .addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            org.eclipse.swt.events.SelectionEvent e) {
                        cmdSearchWidgetSelected();
                    }
                });
    }

    /**
     * This method initializes grpDrugInformation
     */
    private void createGrpDrugInformation() {

        grpDrugInformation = new Group(getShell(), SWT.NONE);
        grpDrugInformation
                .setText("Informações do Medicamento e Instruções de Toma");
        grpDrugInformation.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        grpDrugInformation.setBounds(new Rectangle(85, 180, 350, 127));

        lblDrugName = new Label(grpDrugInformation, SWT.NONE);
        lblDrugName.setBounds(new org.eclipse.swt.graphics.Rectangle(15, 30,
                90, 20));
        lblDrugName.setText("Nome: ");
        lblDrugName.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtDrugName = new Text(grpDrugInformation, SWT.BORDER);
        txtDrugName.setBounds(new org.eclipse.swt.graphics.Rectangle(150, 30,
                184, 20));
        txtDrugName.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtDrugName.setEnabled(false);

        lblTake = new Label(grpDrugInformation, SWT.NONE);
        lblTake.setBounds(new Rectangle(14, 59, 90, 20));
        lblTake.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblTake.setText("Tomar:");
        txtTake = new Text(grpDrugInformation, SWT.BORDER);
        txtTake.setBounds(new Rectangle(149, 59, 60, 20));
        txtTake.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtTake.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtTake.selectAll();
            }

        });
        txtTake.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) {
                    cmdAddDrugWidgetSelected();
                }
            }

        });

        lblDescription = new Label(grpDrugInformation, SWT.NONE);
        lblDescription.setBounds(new Rectangle(219, 59, 120, 20));
        lblDescription.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        lblTimes = new Label(grpDrugInformation, SWT.NONE);
        lblTimes.setBounds(new Rectangle(14, 89, 90, 20));
        lblTimes.setText("Vez(es) :");
        lblTimes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtTimes = new Text(grpDrugInformation, SWT.NONE);
        txtTimes.setBounds(new Rectangle(149, 89, 60, 20));
        txtTimes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtTimes.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtTimes.selectAll();
            }

        });

        txtTimes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) {
                    cmdAddDrugWidgetSelected();
                }
            }

        });

        cmbPeriodoToma = new CCombo(grpDrugInformation, SWT.BORDER);
        cmbPeriodoToma.setBounds(new Rectangle(260, 89, 80, 22));
        cmbPeriodoToma.setEditable(false);
        cmbPeriodoToma.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbPeriodoToma.setBackground(ResourceUtils.getColor(iDartColor.WHITE));
        cmbPeriodoToma.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        CommonObjects.populateTakePeriod(getHSession(), cmbPeriodoToma);
        cmbPeriodoToma.setText(cmbPeriodoToma.getItem(0));

        lblTakePeriod = new Label(grpDrugInformation, SWT.NONE);
        lblTakePeriod.setBounds(new Rectangle(219, 89, 120, 20));
        lblTakePeriod.setText("por");
        lblTakePeriod.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

    }

    /**
     * Clears the patientForm and sets the default values
     */
    public void clearForm() {
        txtTake.setVisible(true);
        lblDescription.setVisible(true);
        lblTake.setVisible(true);

        lblTake.setText("Tomar:");
        lblDescription.setText("");

        txtTake.setText("");
        txtTimes.setText("");
        cmbPeriodoToma.setText(cmbPeriodoToma.getItem(0));
        txtDrugName.setText("");

        newDrug = null;

        enableFields(false);
    }

    private void cmdSearchWidgetSelected() {
        Search drugSearch = new Search(getHSession(), getShell(),
                CommonObjects.ACTIVEDRUG);
        if (drugSearch.getValueSelected() != null) {
            txtDrugName.setText(drugSearch.getValueSelected()[0]);
            loadDrugInformation();
            drugBarCodeSearch.setEnabled(false);
            enableFields(true);
            txtTake.selectAll();
            txtTake.setFocus();

        } else {
            enableFields(false);
            // enableAllControls(false);
        }
    }

    private void cmdAssossiationSearchWidgetSelected() {
        Search drugSearch = new Search(getHSession(), getShell(), true, regimeTerapeutico);
        if (drugSearch.getValueSelected() != null) {
            txtDrugName.setText(drugSearch.getValueSelected()[0]);
            loadDrugInformation();
            drugBarCodeSearch.setEnabled(false);
            enableFields(true);
            txtTake.selectAll();
            txtTake.setFocus();

        } else {
            enableFields(false);
            // enableAllControls(false);
        }
    }

    private void loadDrugInformation() {
        newDrug = DrugManager.getDrug(getHSession(), txtDrugName.getText());
        if (newDrug != null) {
            txtDrugName.setText(newDrug.getName());
            lblDescription.setText(newDrug.getForm().getFormLanguage1());
            cmbPeriodoToma.setText(newDrug.getDefaultTakePeriod());
            lblTake.setText(newDrug.getForm().getActionLanguage1());
            txtTake.setFocus();
            int[] standardDosage = new int[2];

            // is a cream with no amount per time
            if (newDrug.getForm().getFormLanguage1().equals("")) {
                txtTake.setVisible(false);
                lblDescription.setVisible(false);
                lblTakePeriod.setVisible(false);
                lblTake.setVisible(false);
            } else {
                txtTake.setVisible(true);
                lblDescription.setVisible(true);
                lblTakePeriod.setVisible(true);
                lblTake.setVisible(true);
                double takeAmount = newDrug.getDefaultAmnt();
                String takeAmountStr = String.valueOf(takeAmount);

                // if the default take amount is actually a whole number
                // avoid it being displayed as a double
                if ((takeAmountStr.charAt(takeAmountStr.length() - 1) == '0')
                        && (takeAmountStr.charAt(takeAmountStr.length() - 2) == '.')) {
                    txtTake.setText(String.valueOf((int) takeAmount));
                } else {
                    txtTake.setText(takeAmountStr);
                }
            }
            txtTimes.setText(newDrug.getDefaultTimes() == 0 ? "1" : String
                    .valueOf(newDrug.getDefaultTimes()));

            if (standardDosage[0] != 0) {
                btnAddDrug.setFocus();
            } else {
                txtTake.setFocus();
            }
        } else {
            MessageBox m = new MessageBox(getShell(), SWT.ICON_ERROR);
            m.setMessage("O medicamento '" + txtDrugName.getText()
                    + "' não foi encontrado na base de dados.");
            m.setText("Medicamento não encontrado");
            m.open();
            txtDrugName.setText("");
        }
        txtTake.selectAll();
    }

    /**
     * Check if the form is completed before proceeding
     *
     * @return true if all fields are correctly filled in
     */
    private boolean fieldsOk() {
        boolean result = true;
        if (newDrug == null) {
            MessageBox noDrugLoaded = new MessageBox(getShell(), SWT.ICON_ERROR);
            noDrugLoaded.setMessage("Escolher um Medicamento.");
            noDrugLoaded.setText("Campos não Preenchidos");
            noDrugLoaded.open();
            result = false;
            txtDrugName.setFocus();
        }
        if (txtTake.isVisible()) {
            if (txtTake.getText().equals("")) {
                MessageBox take = new MessageBox(getShell(), SWT.ICON_ERROR);
                take.setMessage("Por favor preencher o Campo 'Tomar'.");
                take.setText("Campos não Preenchidos");
                take.open();
                result = false;
                txtTake.setFocus();
            } else {
                Double takeAmount;
                try {
                    takeAmount = Double.valueOf(txtTake.getText());
                    if (!(takeAmount > 0)) {
                        MessageBox notANumber = new MessageBox(getShell(),
                                SWT.ICON_ERROR);
                        notANumber
                                .setMessage("A quantidade a ser inserida no campo 'Tomar' deve ser maior que Zero.");
                        notANumber.setText("Informação Incorrecta");
                        notANumber.open();
                        result = false;
                        txtTake.setFocus();
                    }
                } catch (Exception e) {
                    MessageBox notANumber = new MessageBox(getShell(),
                            SWT.ICON_ERROR);
                    notANumber
                            .setMessage("A informação inserida no campo 'Tomar' não é número.");
                    notANumber.setText("Informação Incorrecta");
                    notANumber.open();
                    result = false;
                    txtTake.setFocus();
                }
            }
        }
        if (txtTimes.getText().trim().equals("")) {
            MessageBox times = new MessageBox(getShell(), SWT.ICON_ERROR);
            times.setMessage("Por favor preencher o Campo 'Vezes de Toma'.");
            times.setText("Campos não Preenchidos");
            times.open();
            result = false;
            txtTimes.setFocus();
        } else {
            try {
                int times = Integer.parseInt(txtTimes.getText().trim());
                if (!(times > 0)) {
                    MessageBox notANumber = new MessageBox(getShell(),
                            SWT.ICON_ERROR);
                    notANumber
                            .setMessage("A quantidade a ser inserida no campo 'Vezes de Toma' deve ser maior que Zero.");
                    notANumber.setText("Informação Incorrecta");
                    notANumber.open();
                    result = false;
                    txtTake.setFocus();
                }
            } catch (NumberFormatException e) {
                MessageBox notANumber = new MessageBox(getShell(),
                        SWT.ICON_ERROR);
                notANumber
                        .setMessage("A informção inserida no campo 'Vezes de Toma' não é número.");
                notANumber.setText("Informação Incorrecta");
                notANumber.open();
                result = false;
                txtTimes.setFocus();
            }
        }
        return result;
    }

    private void cmdAddDrugWidgetSelected() {
        if (fieldsOk()) {

            if (isRegimen) {
                // Create new RegimenDrug
                RegimenDrugs rd = new RegimenDrugs();
                rd.setAmtPerTime(txtTake.isVisible() ? Double.valueOf(
                        txtTake.getText()).doubleValue() : 0);
                rd.setDrug(newDrug);
                rd.setModified('T');
                rd.setTimesPerDay(Integer.parseInt(txtTimes.getText()));
                tableItem.setData(rd);
            } else {
                // Create new PrescribedDrug
                PrescribedDrugs pd = new PrescribedDrugs();
                pd.setAmtPerTime(txtTake.isVisible() ? Double.valueOf(
                        txtTake.getText()).doubleValue() : 0);
                pd.setDrug(newDrug);
                pd.setModified('T');
                pd.setTimesPerDay(Integer.parseInt(txtTimes.getText().trim()));
                pd.setTakePeriod(cmbPeriodoToma.getText());
                tableItem.setData(pd);
            }
            Form f = newDrug.getForm();
            String[] temp = new String[8];
            temp[0] = tableItem.getText(0);
            temp[1] = newDrug.getName();
            temp[2] = f.getActionLanguage1();
            temp[3] = (txtTake.isVisible() ? txtTake.getText() : "");
            temp[4] = f.getFormLanguage1();
            temp[5] = txtTimes.getText();
            temp[6] = "vezes por " + cmbPeriodoToma.getText();
            temp[7] = String.valueOf(newDrug.getPackSize());
            tableItem.setText(temp);
            txtTimes.selectAll();
            closeShell(false);

        }
    }

    private void cmdClearWidgetSelected() {
        clearForm();
        txtDrugName.setFocus();
    }

    private void cmdCancelWidgetSelected() {
        tableItem.dispose();
        closeShell(false);
    }

    /**
     * Method setTableItem.
     *
     * @param tableItem TableItem
     */
    public void setTableItem(TableItem tableItem) {
        this.tableItem = tableItem;
    }

    /**
     * This method initializes compButtons
     */
    @Override
    protected void createCompButtons() {

        btnAddDrug = new Button(getCompButtons(), SWT.NONE);
        btnAddDrug.setSize(150, 30);
        if (isRegimen) {
            btnAddDrug.setText("Adicionar ao Regime Terapeutico");
            btnAddDrug
                    .setToolTipText("Pressione este botão para adicionar o medicamento ao Regime Terapeutico.");
        } else {
            btnAddDrug.setText("Adicionar a Prescrição");
            btnAddDrug
                    .setToolTipText("Clique este botão para adicionar este medicamento a prescrição.");
        }
        btnAddDrug.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnAddDrug
                .addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            org.eclipse.swt.events.SelectionEvent e) {
                        cmdAddDrugWidgetSelected();
                    }
                });

        btnClear = new Button(getCompButtons(), SWT.NONE);
        btnClear
                .setToolTipText("Clique este botão para limpar os campos do formulário.");
        btnClear.setText("Limpar campos");
        btnClear.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnClear
                .addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            org.eclipse.swt.events.SelectionEvent e) {
                        cmdClearWidgetSelected();
                    }
                });

        btnCancel = new Button(getCompButtons(), SWT.NONE);
        btnCancel
                .setToolTipText("Clique este botão cancelar a inserão de dados .");
        btnCancel.setText("Cancelar");
        btnCancel.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnCancel
                .addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            org.eclipse.swt.events.SelectionEvent e) {
                        cmdCancelWidgetSelected();
                    }
                });
    }

    private void enableFields(boolean enable) {
        txtTake.setEnabled(enable);
        txtTimes.setEnabled(enable);
        btnAddDrug.setEnabled(enable);
        cmbPeriodoToma.setEnabled(enable);
//        drugBarCodeSearch.setEnabled(enable);
    }

    @Override
    protected void createCompOptions() {
    }

    @Override
    protected void setLogger() {
        setLog(Logger.getLogger(this.getClass()));
    }

}
