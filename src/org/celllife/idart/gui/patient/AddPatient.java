/*
 * iDART: The Intelligent Dispensing of Antiretroviral Treatment
 * Copyright (C) 2006 Cell-Life
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version
 * 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.celllife.idart.gui.patient;

import model.manager.*;
import model.manager.reports.PatientHistoryReport;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.celllife.function.DateRuleFactory;
import org.celllife.idart.commonobjects.*;
import org.celllife.idart.database.dao.ConexaoJDBC;
import org.celllife.idart.database.hibernate.*;
import org.celllife.idart.database.hibernate.tmp.PackageDrugInfo;
import org.celllife.idart.database.hibernate.util.HibernateUtil;
import org.celllife.idart.gui.misc.iDARTChangeListener;
import org.celllife.idart.gui.patient.tabs.*;
import org.celllife.idart.gui.platform.GenericFormGui;
import org.celllife.idart.gui.prescription.AddPrescription;
import org.celllife.idart.gui.reportParameters.PatientHistory;
import org.celllife.idart.gui.search.PatientSearch;
import org.celllife.idart.gui.utils.*;
import org.celllife.idart.gui.widget.DateButton;
import org.celllife.idart.gui.widget.DateInputValidator;
import org.celllife.idart.integration.eKapa.gui.SearchPatientGui;
import org.celllife.idart.integration.mobilisr.MobilisrManager;
import org.celllife.idart.messages.Messages;
import org.celllife.idart.misc.DateFieldComparator;
import org.celllife.idart.misc.PatientBarcodeParser;
import org.celllife.idart.misc.iDARTUtil;
import org.celllife.idart.print.barcode.Barcode;
import org.celllife.idart.print.label.PatientInfoLabel;
import org.celllife.idart.print.label.PrintLabel;
import org.celllife.idart.rest.ApiAuthRest;
import org.celllife.idart.rest.utils.RestClient;
import org.celllife.mobilisr.client.exception.RestCommandException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static org.celllife.idart.rest.ApiAuthRest.getServerStatus;

public class AddPatient extends GenericFormGui implements iDARTChangeListener {

    private Logger log = Logger.getLogger(AddPatient.class);

    private static final String KEY_MESSAGE = "message"; //$NON-NLS-1$

    private static final String KEY_TITLE = "title"; //$NON-NLS-1$

    private static final String KEY_RESULT = "result"; //$NON-NLS-1$

    private TextAdapter txtPatientId;

    private Button btnSearch;

    private Button btnSearchByName;

    private Button btnEkapaSearch;

    private Label lblOtherPatientsWithThisID;

    private Button btnPatientHistoryReport;

    private Button btnUpdatePrescription;

    private Button btnPrintPatientLabel;

    private Text txtFirstNames;

    private Text txtSurname;

    private Label lblOpenmrsuuid;

    private Text txtOpenmrsuuid;

    private Combo cmbDOBDay;

    private Combo cmbDOBMonth;

    private Combo cmbDOBYear;

    private Combo cmbSex;

    private Button transito;

    private Text txtAge;

    private Label lblPicChild;

    private CCombo cmbClinic;

    // THE GROUPS
    private Group grpParticulars;

    private Patient localPatient;

    /**
     * Are we adding a new patient of updating an existing patient?
     */
    private boolean isAddnotUpdate;

    private Label lblPicUpdatePrescription;

    private Label lblPicPrintPatientLabel;

    private Composite compUpdatePrescription;

    /**
     * Are there other patients that have the same ID as this patient?
     */
    private boolean alternativePatientIds;

    private CCombo cmbEpisodeStopReason;

    private Text txtEpisodeStopNotes;

    private Group grpEpisodes;

    private Label lblPastEpisodeStart;

    private Label lblPastEpisodeDivide;

    private Label lblPastEpisodeStop;

    private Button btnEditEpisodes;

    private CCombo cmbEpisodeStartReason;

    private Label lblPastEpisodeTitle;

    private DateButton btnEpisodeStartDate;

    private DateButton btnEpisodeStopDate;

    private Text txtEpisodeStartNotes;

    /**
     * Is the patient active i.e. do they have an open episode?
     */
    private boolean isPatientActive = false;

    private Composite compPatientInfo;

    private EpisodeViewer epViewer;

    private TabFolder tabbedGroup;

    private Composite cmpTabbedGroup;

    private IPatientTab[] groupTabs;

    private Label lblEpisodeTitle;

    /**
     * If true the user will be asked if they wish to print a new label after
     * they have saved.
     */
    private boolean offerToPrintLabel;

    private Button btnDownRefer;

    private Text txtCellphone;

    private DateButton btnARVStart;

    private String originalPatientId;

    private boolean identifierChangesMade = false;

    private RestClient restClient;

    private String year;

    private String month;

    private Integer day;

    private Date theDate;

    /**
     * Constructor
     *
     * @param parent Shell
     * @param add    boolean true if user is adding a new patient false if user is
     *               updating a patient's details
     */
    public AddPatient(Shell parent, boolean add) {
        super(parent, HibernateUtil.getNewSession());
        isAddnotUpdate = add;
    }

    /**
     * Constructor for AddPatient.
     *
     * @param parent  Shell
     * @param patient Patient
     */
    public AddPatient(Shell parent, Patient patient) {
        super(parent, HibernateUtil.getNewSession());
        localPatient = PatientManager.getPatient(getHSession(), patient
                .getId());
        updateGUIforNewLocalPatient();
    }

    /**
     * This method initializes newPatient
     */
    @Override
    protected void createShell() {
        isAddnotUpdate = (Boolean) GenericFormGui
                .getInitialisationOption(GenericFormGui.OPTION_isAddNotUpdate);
        String shellTxt = isAddnotUpdate ? Messages.getString("patient.screen.title.add") //$NON-NLS-1$
                : Messages.getString("patient.screen.title.update"); //$NON-NLS-1$
        Rectangle bounds = new Rectangle(25, 0, 900, 700);
        buildShell(shellTxt, bounds); // generic shell build
    }

    @Override
    protected void createContents() {
        createCompPatientInfo();
        createCompUpdatePrescription();
        enableFields(false);
        clearForm();
    }

    /**
     * This method initializes compHeader
     */
    @Override
    protected void createCompHeader() {
        String headerTxt = isAddnotUpdate ? Messages
                .getString("patient.screen.title.add") //$NON-NLS-1$
                : Messages.getString("patient.screen.title.update"); //$NON-NLS-1$
        iDartImage icoImage = (isAddnotUpdate ? iDartImage.PATIENTNEW
                : iDartImage.PATIENTUPDATE);
        buildCompHeader(headerTxt, icoImage);
    }

    /**
     * This method initializes compButtons
     */
    @Override
    protected void createCompButtons() {
        // Parent Class generic call
        buildCompButtons();
    }

    /**
     * This method initializes compPatientInfo
     */
    private void createCompPatientInfo() {
        compPatientInfo = new Composite(getShell(), SWT.NONE);
        compPatientInfo.setBounds(new Rectangle(16, 55, 854, 505));
        // create the composites
        createGrpParticulars();
        createGrpEpisodes();
        createCmpTabbedInfo();
        Label lblInstructions = new Label(compPatientInfo, SWT.CENTER);
        lblInstructions.setBounds(new Rectangle(100, 15, 350, 20));
        lblInstructions.setText(Messages.getString("common.label.compulsory")); //$NON-NLS-1$
        lblInstructions.setFont(ResourceUtils
                .getFont(iDartFont.VERASANS_10_ITALIC));
    }

    /**
     * This method initializes grpParticulars
     */
    private void createGrpParticulars() {

        int col2x = 105;

        // grpParticulars
        grpParticulars = new Group(compPatientInfo, SWT.NONE);
        grpParticulars.setBounds(new Rectangle(30, 40, 400, 255));
        grpParticulars.setText(Messages.getString("patient.group.particulars")); //$NON-NLS-1$
        grpParticulars.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        // Patient ID
        Label lblPatientId = new Label(grpParticulars, SWT.NONE);
        lblPatientId.setBounds(new Rectangle(7, 20, 84, 20));
        lblPatientId.setText(Messages.getString("common.compulsory.marker") + Messages.getString("patient.label.patientid")); //$NON-NLS-1$ //$NON-NLS-2$
        lblPatientId.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        txtPatientId = new TextAdapter(grpParticulars, SWT.BORDER);
        txtPatientId.setBounds(new Rectangle(col2x, 20, 150, 20));
        txtPatientId.setData(iDartProperties.SWTBOT_KEY, "txtPatientId"); //$NON-NLS-1$
        txtPatientId.setFocus();
        txtPatientId.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtPatientId.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if ((btnSearch != null) && (btnSearch.getEnabled())) {
                    if ((e.character == SWT.CR)
                            || (e.character == (char) iDartProperties.intValueOfAlternativeBarcodeEndChar)) {
                        cmdSearchWidgetSelected();
                    }
                }
            }
        });

        if (isAddnotUpdate) {
            txtPatientId.setEnabled(false);
        }

        btnSearch = new Button(grpParticulars, SWT.NONE);
        btnSearch.setBounds(new Rectangle(270, 20, 119, 28));
		/*if(iDartProperties.country.equalsIgnoreCase("Nigeria")){
			btnSearch.setBounds(new Rectangle(270, 47, 110, 28));
		} else {
			btnSearch.setBounds(new Rectangle(270, 20, 110, 28));
		}*/

        btnSearchByName = new Button(grpParticulars, SWT.NONE);
        btnSearchByName.setBounds(new Rectangle(270, 20, 119, 28));

        if (!isAddnotUpdate) {
            btnSearchByName.setVisible(false);
        }

        if (isAddnotUpdate) {
            btnSearch.setText(Messages.getString("patient.button.editid")); //$NON-NLS-1$
            btnSearch.setToolTipText(Messages.getString("patient.button.editid.tooltip")); //$NON-NLS-1$

            btnSearchByName.setText(Messages.getString("patient.button.editid.nome")); //$NON-NLS-1$
            btnSearchByName.setToolTipText(Messages.getString("patient.button.editid.tooltip.nome")); //$NON-NLS-1$
        } else {
            btnSearch.setText(Messages.getString("patient.button.search")); //$NON-NLS-1$
            btnSearch
                    .setToolTipText(Messages.getString("patient.button.search.tooltip")); //$NON-NLS-1$
        }

        btnSearch.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cmdSearchWidgetSelected();
            }
        });

        btnSearchByName.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnSearchByName.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cmdSearchWidgetSelectedSearchByName();
            }
        });

        btnEkapaSearch = new Button(grpParticulars, SWT.NONE);
        btnEkapaSearch.setBounds(new Rectangle(270, 50, 110, 28));
        btnEkapaSearch.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnEkapaSearch.setText(Messages.getString("patient.button.ekapasearch")); //$NON-NLS-1$
        if (!iDartProperties.isEkapaVersion || isAddnotUpdate) {
            btnEkapaSearch.setVisible(false);
        }

        btnEkapaSearch.setToolTipText(Messages.getString("patient.button.ekapasearch.tooltip")); //$NON-NLS-1$
        btnEkapaSearch.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                cmdEkapaSearchWidgetSelected();
            }
        });

        // FirstNames
        Label lblFirstNames = new Label(grpParticulars, SWT.NONE);
        lblFirstNames.setBounds(new Rectangle(7, 45, 84, 20));
        lblFirstNames.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblFirstNames
                .setText(Messages.getString("common.compulsory.marker") + Messages.getString("patient.label.firstname")); //$NON-NLS-1$ //$NON-NLS-2$
        txtFirstNames = new Text(grpParticulars, SWT.BORDER);
        txtFirstNames.setBounds(new Rectangle(col2x, 45, 150, 20));
        txtFirstNames.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        // Surname
        Label lblSurname = new Label(grpParticulars, SWT.NONE);
        lblSurname.setBounds(new Rectangle(7, 70, 84, 20));
        lblSurname.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblSurname.setText(Messages.getString("common.compulsory.marker") + Messages.getString("patient.label.surname")); //$NON-NLS-1$ //$NON-NLS-2$
        txtSurname = new Text(grpParticulars, SWT.BORDER);
        txtSurname.setBounds(new Rectangle(col2x, 70, 150, 20));
        txtSurname.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        lblOtherPatientsWithThisID = new Label(grpParticulars, SWT.NONE);
        lblOtherPatientsWithThisID.setBounds(new Rectangle(355, 140, 40, 40));
        lblOtherPatientsWithThisID.setImage(ResourceUtils
                .getImage(iDartImage.PATIENTDUPLICATES_30X26));
        lblOtherPatientsWithThisID.setVisible(false);

        // Date of Birth
        Label lbldob = new Label(grpParticulars, SWT.NONE);
        lbldob.setBounds(new Rectangle(7, 95, 84, 20));
        lbldob.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lbldob.setText(Messages.getString("common.compulsory.marker") + Messages.getString("patient.label.dob")); //$NON-NLS-1$ //$NON-NLS-2$

        cmbDOBDay = new Combo(grpParticulars, SWT.BORDER);
        cmbDOBDay.setBounds(new Rectangle(col2x, 95, 50, 18));
        cmbDOBDay.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbDOBDay.setBackground(ResourceUtils.getColor(iDartColor.WHITE));
        cmbDOBDay.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        cmbDOBDay.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String theText = cmbDOBDay.getText();
                if (cmbDOBDay.indexOf(theText) == -1) {
                    cmbDOBDay.setText(cmbDOBDay.getItem(0));
                }
            }
        });

        cmbDOBMonth = new Combo(grpParticulars, SWT.BORDER);
        cmbDOBMonth.setBounds(new Rectangle(col2x + 50, 95, 97, 18));
        cmbDOBMonth.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbDOBMonth.setBackground(ResourceUtils.getColor(iDartColor.WHITE));
        cmbDOBMonth.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        cmbDOBMonth.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String theText = cmbDOBMonth.getText();
                if (theText.length() > 2) {
                    String s = theText.substring(0, 1);
                    String t = theText.substring(1, theText.length());
                    theText = s.toUpperCase() + t;
                    String[] items = cmbDOBMonth.getItems();
                    for (int i = 0; i < items.length; i++) {
                        if (items[i].substring(0, 3).equalsIgnoreCase(theText)) {
                            cmbDOBMonth.setText(items[i]);
                            cmbDOBYear.setFocus();
                        }
                    }
                }
            }
        });

        cmbDOBYear = new Combo(grpParticulars, SWT.BORDER);
        cmbDOBYear.setBounds(new Rectangle(col2x + 148, 95, 60, 18));
        cmbDOBYear.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbDOBYear.setBackground(ResourceUtils.getColor(iDartColor.WHITE));
        cmbDOBYear.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        cmbDOBYear.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String theText = cmbDOBYear.getText();
                if ((cmbDOBYear.indexOf(theText) == -1)
                        && (theText.length() >= 4)) {
                    cmbDOBYear.setText(EMPTY);
                }
            }
        });

        cmbDOBDay.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                cmdUpdateAge();
            }
        });
        cmbDOBMonth.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                cmdUpdateAge();
            }
        });
        cmbDOBYear.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                cmdUpdateAge();
            }
        });

        // populate the comboboxes
        ComboUtils.populateDateCombos(cmbDOBDay, cmbDOBMonth, cmbDOBYear,
                false, false);
        cmbDOBDay.setVisibleItemCount(cmbDOBDay.getItemCount());
        cmbDOBMonth.setVisibleItemCount(cmbDOBMonth.getItemCount());
        cmbDOBYear.setVisibleItemCount(31);

        // Age
        Label lblAge = new Label(grpParticulars, SWT.NONE);
        lblAge.setBounds(new Rectangle(col2x + 212, 95, 33, 20));
        lblAge.setText(Messages.getString("patient.label.age")); //$NON-NLS-1$
        lblAge.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtAge = new Text(grpParticulars, SWT.BORDER);
        txtAge.setBounds(new Rectangle(col2x + 249, 95, 35, 20));
        txtAge.setEditable(false);
        txtAge.setEnabled(false);
        txtAge.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));


        // Sex
        Label lblSex = new Label(grpParticulars, SWT.NONE);
        lblSex.setBounds(new Rectangle(7, 120, 84, 20));
        lblSex.setText(Messages.getString("patient.label.sex")); //$NON-NLS-1$
        lblSex.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        cmbSex = new Combo(grpParticulars, SWT.BORDER);
        cmbSex.setBounds(new Rectangle(col2x, 120, 150,
                18));
        cmbSex.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbSex.setBackground(ResourceUtils.getColor(iDartColor.WHITE));
        cmbSex.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        // cmbSex.setEditable(false);
        //cmbSex.add(Messages.getString("common.unknown")); //$NON-NLS-1$
        cmbSex.add(Messages.getString("patient.sex.female")); //$NON-NLS-1$
        cmbSex.add(Messages.getString("patient.sex.male")); //$NON-NLS-1$
        //cmbSex.setText(Messages.getString("common.unknown")); //$NON-NLS-1$
        cmbSex.select(0);
        cmbSex.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                e.doit = false;
                Character keyPressed = new Character(Character
                        .toLowerCase(e.character));
                getLog().debug("The char pressed in cmbSex: " + keyPressed); //$NON-NLS-1$
                if (Character.isLetter(keyPressed)) {
                    if (keyPressed.equals('f')) {
                        cmbSex.select(1);
                    } else if (keyPressed.equals('m')) {
                        cmbSex.select(2);
                    } else {
                        cmbSex.select(0);
                    }
                    updateClinicInfoTab();
                }
            }
        });

        /*
         * add TraverseListener to allow traversal out of combo see JavaDoc for
         * org.eclipse.swt.events.KeyEvent
         */
        cmbSex.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT
                        || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                } else {
                    e.doit = false;
                }
            }

        });
        cmbSex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                updateClinicInfoTab();
            }
        });

        // Child Icon
        lblPicChild = new Label(grpParticulars, SWT.NONE);
        lblPicChild.setBounds(new Rectangle(255, 110, 50, 43));
        lblPicChild.setImage(ResourceUtils.getImage(iDartImage.CHILD_50X43));
        lblPicChild.setVisible(false);

        // Phone Cell
        Label lblPhoneCell = new Label(grpParticulars, SWT.NONE);
        lblPhoneCell.setBounds(new Rectangle(7, 145, 85, 20));
        lblPhoneCell.setText(Messages.getString("patient.label.cellphone")); //$NON-NLS-1$
        lblPhoneCell.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtCellphone = new Text(grpParticulars, SWT.BORDER);
        txtCellphone.setBounds(new Rectangle(col2x, 145, 150, 20));
        txtCellphone.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtCellphone.setEnabled(false);

        Label lblARVStartDate = new Label(grpParticulars, SWT.NONE);
        lblARVStartDate.setBounds(new Rectangle(7, 170, 90, 20));
        lblARVStartDate.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblARVStartDate.setText(Messages.getString("patient.label.arvstartdate")); //$NON-NLS-1$

        btnARVStart = new DateButton(grpParticulars, DateButton.ZERO_TIMESTAMP, new DateInputValidator(DateRuleFactory.beforeNowInclusive(true)));
        btnARVStart.setBounds(new Rectangle(col2x, 170, 150, 20));
        btnARVStart.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnARVStart.setText(Messages.getString("common.unknown")); //$NON-NLS-1$

        // Openmrs UUID
        lblOpenmrsuuid = new Label(grpParticulars, SWT.NONE);
        lblOpenmrsuuid.setBounds(new org.eclipse.swt.graphics.Rectangle(7, 200,
                105, 20));
        lblOpenmrsuuid.setText(Messages.getString("AddressTab.openmrs.uuid.label")); //$NON-NLS-1$
        lblOpenmrsuuid.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblOpenmrsuuid.setVisible(false);
        txtOpenmrsuuid = new Text(grpParticulars, SWT.BORDER);
        txtOpenmrsuuid.setBounds(new org.eclipse.swt.graphics.Rectangle(col2x, 200, 220, 20));
        txtOpenmrsuuid.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        txtOpenmrsuuid.setEnabled(false);
        txtOpenmrsuuid.setVisible(false);

        btnPatientHistoryReport = new Button(grpParticulars, SWT.NONE);
        btnPatientHistoryReport.setBounds(new Rectangle(260, 115, 40, 40));
        btnPatientHistoryReport.setEnabled(false);
        btnPatientHistoryReport
                .setToolTipText(Messages.getString("patient.button.report.tooltip")); //$NON-NLS-1$
        btnPatientHistoryReport.setImage(ResourceUtils
                .getImage(iDartImage.REPORT_PATIENTHISTORY_30X26));

        btnPatientHistoryReport.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent mu) {
                cmdPatientHistoryWidgetSelected();
            }
        });

    }

    private void updateClinicInfoTab() {
        if (cmbSex.getText().equalsIgnoreCase(Messages.getString("patient.sex.female"))) { //$NON-NLS-1$
            ((ClinicInfoTab) groupTabs[2]).setPatientIsFemale(true);
            groupTabs[2].enable(true, null);
        } else if (cmbSex.getText().equalsIgnoreCase(Messages.getString("patient.sex.male"))) { //$NON-NLS-1$
            ((ClinicInfoTab) groupTabs[2]).setPatientIsFemale(false);
            groupTabs[2].enable(false, null);
        } else {
            ((ClinicInfoTab) groupTabs[2]).setPatientIsFemale(false);
            groupTabs[2].enable(false, null);
        }
    }

    /**
     * This method initializes the grpEpisodes
     */
    private void createGrpEpisodes() {
        int spacer = 7;
        int more = 1;
        grpEpisodes = new Group(compPatientInfo, SWT.NONE);
        grpEpisodes.setText(Messages.getString("patient.group.episodes")); //$NON-NLS-1$
        grpEpisodes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        grpEpisodes.setBounds(new Rectangle(440, 40, 380, 255));
        grpEpisodes.setBackground(ResourceUtils
                .getColor(iDartColor.WIDGET_BACKGROUND));

        spacer += more;
        lblEpisodeTitle = new Label(grpEpisodes, SWT.WRAP);
        lblEpisodeTitle.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8_ITALIC));
        lblEpisodeTitle.setBounds(10, 12 + spacer, 150, 26);
        spacer += more;

        String labelTitle;
        if (isAddnotUpdate) {
            labelTitle = Messages.getString("patient.label.episode.new"); //$NON-NLS-1$
        } else {
            labelTitle = Messages.getString("patient.label.episode.current"); //$NON-NLS-1$
        }
        lblEpisodeTitle.setText(labelTitle);

        if (iDartProperties.showDownReferButton) {
            btnDownRefer = new Button(grpEpisodes, SWT.PUSH);
            btnDownRefer.setBounds(205, 6 + spacer, 170, 25);
            btnDownRefer.setText(Messages.getString("patient.button.downrefer")); //$NON-NLS-1$
            btnDownRefer.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
            btnDownRefer.setEnabled(false);
            btnDownRefer.setToolTipText(Messages.getString("patient.button.downrefer.tooltip")); //$NON-NLS-1$
            btnDownRefer.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        SelectionEvent e) {
                    cmdDownreferSelected();
                }
            });
        }

        Label lblEpisodeStart = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStart.setBounds(new Rectangle(16, 45 + spacer, 84, 20));
        spacer += more;
        lblEpisodeStart.setText(Messages.getString("patient.label.episode.start")); //$NON-NLS-1$
        lblEpisodeStart.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        cmbEpisodeStartReason = new CCombo(grpEpisodes, SWT.BORDER);
        cmbEpisodeStartReason.setBounds(new Rectangle(110, 40 + spacer, 135, 20));
        spacer += more;
        cmbEpisodeStartReason.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbEpisodeStartReason.setEnabled(true);
        cmbEpisodeStartReason.setEditable(false);
        cmbEpisodeStartReason.setBackground(ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        cmbEpisodeStartReason.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        CommonObjects.populateActivationReasons(getHSession(), cmbEpisodeStartReason);
        cmbEpisodeStartReason.setText(EMPTY);
        cmbEpisodeStartReason.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String startReason = cmbEpisodeStartReason.getText();
                if (startReason.trim().isEmpty()) {
                    btnEpisodeStartDate.setDate(null);
                    btnEpisodeStartDate.setText(Messages.getString("patient.label.startdate")); //$NON-NLS-1$
                    txtEpisodeStartNotes.setText(EMPTY);
                    // if you don't have a start, you can't have an end
                    btnEpisodeStopDate.clearDate();
                    btnEpisodeStopDate.setText(Messages.getString("patient.label.stopdate")); //$NON-NLS-1$
                    cmbEpisodeStopReason.setText(EMPTY);
                    cmbClinic.setText(EMPTY);
                } else {
                    cmbClinic.setEnabled(true);

                    String clinicName = null;
                    if (localPatient != null) {
                        Episode episode = localPatient.getMostRecentEpisode();
                        if (episode != null && episode.isOpen()) {
                            if (btnEpisodeStartDate.getDate() == null)
                                btnEpisodeStartDate.setDate(episode.getStartDate());

                            txtEpisodeStartNotes.setText(episode.getStartNotes());

                            Clinic clinic = episode.getClinic();
                            if (clinic != null) {
                                clinicName = clinic.getClinicName();
                            }
                        }
                    }
                    if (clinicName == null) {
                        clinicName = AdministrationManager
                                .getDefaultClinicName(getHSession());
                    }
                    cmbClinic.setText(clinicName);
                }
            }
        });

        Label lblEpisodeStartDivide = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStartDivide.setText(Messages.getString("patient.label.on")); //$NON-NLS-1$
        lblEpisodeStartDivide.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblEpisodeStartDivide.setBounds(253, 45 + spacer - 5, 20, 15);
        spacer += more;

        btnEpisodeStartDate = new DateButton(grpEpisodes,
                DateButton.ZERO_TIMESTAMP, new DateInputValidator(
                DateRuleFactory.beforeNowInclusive(true)));
        btnEpisodeStartDate.setBounds(275, 40 + spacer - 5, 100, 25);
        spacer += more;
        btnEpisodeStartDate.setText(Messages.getString("patient.label.startdate")); //$NON-NLS-1$
        btnEpisodeStartDate.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnEpisodeStartDate.setToolTipText(Messages.getString("patient.button.startdate.tooltip")); //$NON-NLS-1$

        Label lblEpisodeStartNotes = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStartNotes.setBounds(new Rectangle(16, 68 + spacer - 3, 84, 20));
        spacer += more;
        lblEpisodeStartNotes.setText(Messages.getString("patient.label.startnotes")); //$NON-NLS-1$
        lblEpisodeStartNotes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        txtEpisodeStartNotes = new Text(grpEpisodes, SWT.BORDER);
        txtEpisodeStartNotes.setBackground(ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        txtEpisodeStartNotes.setBounds(new Rectangle(110, 65 + spacer - 3, 265, 20));
        spacer += more;
        txtEpisodeStartNotes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        // Distribution Clinic
        Label lblDistrib = new Label(grpEpisodes, SWT.NONE);
        lblDistrib.setBounds(new Rectangle(16, 92 + spacer, 84, 20));
        lblDistrib.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblDistrib.setText(Messages.getString("patient.label.clinic")); //$NON-NLS-1$
        cmbClinic = new CCombo(grpEpisodes, SWT.BORDER);
        cmbClinic.setBackground(ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        cmbClinic.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        cmbClinic.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbClinic.setBounds(new Rectangle(110, 87 + spacer, 135, 20));
        CommonObjects.populateClinics(getHSession(), cmbClinic);
        cmbClinic.setEditable(false);
        cmbClinic.setEnabled(false);

        Label lblEpisodeStopDate = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStopDate.setBounds(new Rectangle(16, 118 + spacer, 84, 20));
        lblEpisodeStopDate.setText(Messages.getString("patient.label.stop")); //$NON-NLS-1$
        lblEpisodeStopDate.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        cmbEpisodeStopReason = new CCombo(grpEpisodes, SWT.BORDER);
        cmbEpisodeStopReason.setBackground(ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        cmbEpisodeStopReason.setForeground(ResourceUtils.getColor(iDartColor.BLACK));
        cmbEpisodeStopReason.setBounds(new Rectangle(110, 113 + spacer, 135, 20));
        cmbEpisodeStopReason.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        cmbEpisodeStopReason.setEnabled(true);
        cmbEpisodeStopReason.setEditable(false);
        CommonObjects.populateDeactivationReasons(getHSession(), cmbEpisodeStopReason);
        cmbEpisodeStopReason.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cmbEpisodeStopReason.getText().trim().isEmpty()) {
                    btnEpisodeStopDate.clearDate();
                    btnEpisodeStopDate.setText(Messages.getString("patient.label.stopdate")); //$NON-NLS-1$
                } else {
                    // user intends to close the episode. Lets check if the
                    // patient has any uncollected packages
                    Packages uncollectedPackage = PackageManager.getMostRecentUncollectedPackage(getHSession(),
                            localPatient);
                    if (uncollectedPackage != null) {
                        MessageBox mbox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
                        mbox.setText(Messages.getString("patient.error.episode.close.uncollected.package.title")); //$NON-NLS-1$
                        mbox.setMessage(MessageFormat.format(Messages.getString("patient.error.episode.close.uncollected.package"), //$NON-NLS-1$
                                localPatient.getPatientId(), iDARTUtil.format(uncollectedPackage.getPackDate())));
                        // reset episode fields
                        if (mbox.open() == SWT.NO) {
                            btnEpisodeStopDate.clearDate();
                            btnEpisodeStopDate.setText(Messages.getString("patient.label.stopdate")); //$NON-NLS-1$
                            cmbEpisodeStopReason.select(cmbEpisodeStopReason.indexOf(EMPTY));
                            return;
                        }
                    }
                    if (btnEpisodeStopDate.getDate() == null) {
                        btnEpisodeStopDate.setDate(new Date());
                    }
                }
            }
        });

        Label lblEpisodeStopDivide = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStopDivide.setText(Messages.getString("patient.label.on")); //$NON-NLS-1$
        lblEpisodeStopDivide.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblEpisodeStopDivide.setBounds(253, 118 + spacer, 20, 15);
        spacer += more;

        btnEpisodeStopDate = new DateButton(grpEpisodes, DateButton.ZERO_TIMESTAMP, null);
        btnEpisodeStopDate.setBounds(275, 113 + spacer - 5, 100, 25);
        spacer += more;
        btnEpisodeStopDate.setText(Messages.getString("patient.label.stopdate")); //$NON-NLS-1$
        btnEpisodeStopDate.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnEpisodeStopDate.setToolTipText(Messages.getString("patient.button.stopdate")); //$NON-NLS-1$

        Label lblEpisodeStopNotes = new Label(grpEpisodes, SWT.NONE);
        lblEpisodeStopNotes.setBounds(new Rectangle(16, 140 + spacer, 84, 20));
        spacer += more;
        lblEpisodeStopNotes.setText(Messages.getString("patient.label.stopnotes")); //$NON-NLS-1$
        lblEpisodeStopNotes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        txtEpisodeStopNotes = new Text(grpEpisodes, SWT.BORDER);
        txtEpisodeStopNotes.setBackground(ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        txtEpisodeStopNotes.setBounds(new Rectangle(110, 135 + spacer, 265, 20));
        spacer += more;
        txtEpisodeStopNotes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));

        // Previous episodes
        lblPastEpisodeTitle = new Label(grpEpisodes, SWT.NONE);
        lblPastEpisodeTitle.setText(Messages.getString("patient.label.episode.previous")); //$NON-NLS-1$
        lblPastEpisodeTitle.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8_ITALIC));
        lblPastEpisodeTitle.setBounds(10, 160 + spacer, 190, 13);
        spacer += more;

        lblPastEpisodeStart = new Label(grpEpisodes, SWT.NONE);
        lblPastEpisodeStart.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblPastEpisodeStart.setBounds(16, 183 + spacer - 4, 164, 13);
        spacer += more;

        lblPastEpisodeDivide = new Label(grpEpisodes, SWT.NONE);
        lblPastEpisodeDivide.setText(Messages.getString("patient.label.episode.divide")); //$NON-NLS-1$
        lblPastEpisodeDivide.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblPastEpisodeDivide.setBounds(180, lblPastEpisodeStart.getBounds().y, 20, 13);
        lblPastEpisodeDivide.setVisible(false);

        lblPastEpisodeStop = new Label(grpEpisodes, SWT.NONE);
        lblPastEpisodeStop.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        lblPastEpisodeStop.setBounds(210, lblPastEpisodeStart.getBounds().y, 164, 13);
        spacer += more;

        btnEditEpisodes = new Button(grpEpisodes, SWT.PUSH);
        btnEditEpisodes.setBounds(70, 200 + spacer, 255, 28);
        btnEditEpisodes.setText(Messages.getString("patient.button.episdeviewer")); //$NON-NLS-1$
        btnEditEpisodes.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        btnEditEpisodes.setToolTipText(Messages.getString("patient.button.episdeviewer.tooltip")); //$NON-NLS-1$
        btnEditEpisodes.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cmdEditEpisodesSelected();
            }
        });
    }

    protected void cmdDownreferSelected() {
        boolean confirm = true;
        if (changesMadeToPatient()) {
            confirm = MessageDialog.openConfirm(getShell(),
                    Messages.getString("patient.warning.losechanges.title"), //$NON-NLS-1$
                    Messages.getString("patient.warning.losechanges")); //$NON-NLS-1$
        }
        if (confirm) {
            DownReferDialog drd = new DownReferDialog(getShell(), getHSession(), localPatient);
            drd.openAndWait();
            reloadPatient(false);
        }
    }

    private void cmdEditEpisodesSelected() {
        epViewer.openViewer();
    }

    /**
     * Clears the patientForm interface elements.
     */
    @Override
    public void clearForm() {
        identifierChangesMade = false;
        originalPatientId = null;
        txtFirstNames.setText(EMPTY);
        txtSurname.setText(EMPTY);
        cmbDOBDay.setText(EMPTY);
        cmbDOBMonth.setText(EMPTY);
        cmbDOBYear.setText(EMPTY);
        //cmbSex.setText(Messages.getString("common.unknown")); //$NON-NLS-1$
        cmbSex.setText(EMPTY);
        txtAge.setText(EMPTY);
        txtCellphone.setText(EMPTY);
        txtOpenmrsuuid.setText(EMPTY);
        txtPatientId.setText(EMPTY);
        txtPatientId.setFocus();
//		txtPatientId.setEditable(true);
//		txtPatientId.setEnabled(true);

        cmbClinic.setText(EMPTY);

        lblPastEpisodeTitle.setText(Messages.getString("patient.label.episode.previous")); //$NON-NLS-1$

        if (isAddnotUpdate) {
            lblEpisodeTitle.setText(Messages.getString("patient.label.episode.new")); //$NON-NLS-1$
        } else {
            lblEpisodeTitle.setText(Messages.getString("patient.label.episode.current")); //$NON-NLS-1$
        }

        lblEpisodeTitle.setForeground(ResourceUtils
                .getColor(iDartColor.BLACK));
        cmbEpisodeStartReason.setText(EMPTY);
        btnEpisodeStartDate.setText(Messages.getString("patient.label.startdate")); //$NON-NLS-1$
        txtEpisodeStartNotes.setText(EMPTY);
        cmbEpisodeStopReason.setText(EMPTY);
        btnEpisodeStopDate.setText(Messages.getString("patient.label.stopdate")); //$NON-NLS-1$
        txtEpisodeStopNotes.setText(EMPTY);
        lblPastEpisodeStart.setText(EMPTY);
        lblPastEpisodeStart.setForeground(ResourceUtils
                .getColor(iDartColor.BLACK));
        lblPastEpisodeStop.setText(EMPTY);
        lblPastEpisodeDivide.setVisible(false);

        btnARVStart.setDate(null);

        // clear data from the tabs
        for (IPatientTab tab : groupTabs) {
            tab.clear();
        }
        // The default tab is the Patient History Tab.
        tabbedGroup.setSelection(3);
    }

    /**
     * checks the form for valid fields entries
     *
     * @return true if the required fields are filled in
     */
    @Override
    protected boolean fieldsOk() {

        boolean result = true;
        boolean checkOpenmrs = true;
        Prescription currentPrescription = null;
        String title = EMPTY;
        String message = EMPTY;
        Date episodeStopDate = btnEpisodeStopDate.getDate();
        Date episodeStartDate = btnEpisodeStartDate.getDate();

        if (CentralizationProperties.centralization.equalsIgnoreCase("off"))
            checkOpenmrs = true;
        else if (CentralizationProperties.pharmacy_type.equalsIgnoreCase("F")
                || CentralizationProperties.pharmacy_type.equalsIgnoreCase("P"))
            checkOpenmrs = false;

        if (!isAddnotUpdate) {
            currentPrescription = localPatient.getCurrentPrescription(iDartProperties.SERVICOTARV);
        }

        if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade"))
            if (checkOpenmrs && isAddnotUpdate) {
                try {
                    if (getServerStatus(JdbcProperties.urlBase).contains("Red")) {
                        log.trace(new Date() + " :Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador");
                        showMessage(MessageDialog.WARNING, "Servidor OpenMRS Offline", "Por favor, verifique a conexão com OpenMRS para efectuar esta operação.");
                        result = false;
                    } else {
                        if (checkOpenmrs) {
                            User currentUser = LocalObjects.getUser(HibernateUtil.getNewSession());

                            assert currentUser != null;
                            if (ApiAuthRest.loginOpenMRS(currentUser)) {

                                restClient = new RestClient();
                                String patientId = txtPatientId.getText().toUpperCase().trim();

                                //Verificar se o NID existe no OpenMRS
                                String openMrsResource = restClient.getOpenMRSResource(iDartProperties.REST_GET_PATIENT + StringUtils.replace(patientId, " ", "%20"));

                                if (openMrsResource.length() == 14) {
                                    title = Messages.getString("Informação não encontrada");
                                    message = Messages.getString("NID inserido não existe no OpenMRS");
                                    txtPatientId.setFocus();
                                    result = false;
                                }
                            } else {
                                log.error("O Utilizador " + currentUser.getUsername() + " não se encontra no OpenMRS ou serviço rest no OpenMRS não está em funcionamento.");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        if (txtPatientId.getText().trim().isEmpty()) {
            title = Messages.getString("patient.error.missingfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.patientid.blank"); //$NON-NLS-1$
            txtPatientId.setFocus();
            result = false;
        }

        if (cmbSex.getText().trim().isEmpty()) { //$NON-NLS-1$

            title = Messages.getString("Sexo vazio"); //$NON-NLS-1$
            message = Messages.getString("Seleccione o sexo do paciente"); //$NON-NLS-1$
            txtPatientId.setFocus();
            result = false;
        }

        // Check if openmrs uuid is valid
        if (result && !txtOpenmrsuuid.getText().isEmpty()) {
            if (!CommonObjects.isUUID(txtOpenmrsuuid.getText()) && checkOpenmrs) { //$NON-NLS-1$
                title = Messages.getString("AddressTab.error.invalid-uuid.title"); //$NON-NLS-1$
                message = Messages.getString("AddressTab.error.invalid-uuid.msg"); //$NON-NLS-1$
                txtOpenmrsuuid.setFocus();
                result = false;
            }
        } else if (result && txtFirstNames.getText().trim().isEmpty()) {
            title = Messages.getString("patient.error.missingfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.firstname.blank"); //$NON-NLS-1$
            txtFirstNames.setFocus();
            result = false;
        } else if (txtSurname.getText().trim().isEmpty()) {
            title = Messages.getString("patient.error.missingfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.surname.blank"); //$NON-NLS-1$
            txtSurname.setFocus();
            result = false;
        } else if (cmbDOBDay.getText().isEmpty()
                || cmbDOBMonth.getText().isEmpty()
                || cmbDOBYear.getText().isEmpty()) {
            title = Messages.getString("patient.error.missingfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.dob.blank"); //$NON-NLS-1$
            cmbDOBDay.setFocus();
            result = false;
        } else if (!dateOkay(cmbDOBDay.getText(), cmbDOBMonth.getText(),
                cmbDOBYear.getText())) {
            title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.dob.invalid"); //$NON-NLS-1$
            cmbDOBDay.setFocus();
            result = false;
        } else if (!iDARTUtil.validBirthDate(cmbDOBDay.getText(), cmbDOBMonth
                .getText(), cmbDOBYear.getText())) {
            title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.dobInFuture"); //$NON-NLS-1$
            cmbDOBDay.setFocus();
            result = false;
        } else if (isPatientActive && (currentPrescription != null) && (episodeStopDate != null)) {
            // if the current prescription's capture date is after the episode
            // stop date
            if (iDARTUtil.before(episodeStopDate, currentPrescription.getDate())) {
                title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
                message = MessageFormat.format(Messages.getString("patient.error.episodeStopBeforePrescription"), //$NON-NLS-1$
                        localPatient.getPatientId(), iDARTUtil.format(currentPrescription.getDate()));
                btnEpisodeStopDate.setFocus();
                result = false;
            }
        } else if (episodeStartDate != null && !iDARTUtil.isInPast(episodeStartDate)) {
            title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.episodeStartInFuture"); //$NON-NLS-1$
            btnEpisodeStopDate.setFocus();
            result = false;

        } else if (episodeStopDate != null && cmbClinic.getText().isEmpty()) {
            title = Messages.getString("patient.error.missingfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.clinic.blank"); //$NON-NLS-1$
            cmbClinic.setFocus();
            result = false;
        } else if (episodeStopDate != null
                && !iDARTUtil.isInPast(episodeStopDate)) {
            title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.episodeStopInFuture"); //$NON-NLS-1$
            btnEpisodeStopDate.setFocus();
            result = false;
        } else if (episodeStopDate != null && episodeStartDate != null
                && episodeStartDate.after(episodeStopDate)) {
            title = Messages.getString("patient.error.invalidfield.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.episodeStopBeforeStart"); //$NON-NLS-1$
            btnEpisodeStopDate.setFocus();
            result = false;
        }
        /*
         * check that the ID is not too long max length is determined by the
         * longest possible barcode length the longest barcode including the
         * patientID is the package cover label, so the length of a possible
         * package cover label for this patient must be checked.
         */
		/*else if (!patIdLengthOk()) {
			title = Messages.getString("patient.error.patientIdTooLong.title"); //$NON-NLS-1$
			message = MessageFormat.format(Messages.getString("patient.error.patientIdTooLong"), //$NON-NLS-1$
					Barcode.getLengthForCurrentOS() - 10);
			txtPatientId.setFocus();
			result = false;
		}*/

        if (localPatient.getPatientIdentifiers().isEmpty()) {
            title = Messages.getString("patient.error.patientIdsEmpty.title"); //$NON-NLS-1$
            message = MessageFormat.format(Messages.getString("patient.error.patientIdsEmpty"), //$NON-NLS-1$
                    Barcode.getLengthForCurrentOS() - 10);
            txtPatientId.setFocus();
            result = false;
        }

        // new patients must have an episode
        boolean emptyEpisodeStart = episodeStartDate == null && cmbEpisodeStartReason.getText().trim().isEmpty();
        if ((localPatient.getId() == -1) && emptyEpisodeStart) {
            title = Messages.getString("patient.error.episodeEmpty.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.episodeEmpty"); //$NON-NLS-1$
            result = false;
        } else if ((localPatient.getMostRecentEpisode() != null && localPatient
                .getMostRecentEpisode().isOpen()) && emptyEpisodeStart) {
            title = Messages.getString("patient.error.episodeStartDateNull.title"); //$NON-NLS-1$
            message = Messages.getString("patient.error.episodeStartDateNull"); //$NON-NLS-1$
            result = false;
        }

        if (result) {
            if (!emptyEpisodeStart) {
                Map<String, String> data = PatientManager.validateEpisode(
                        episodeStartDate, cmbEpisodeStartReason.getText().trim(),
                        episodeStopDate, cmbEpisodeStopReason.getText().trim());
                result = Boolean.valueOf(data.get(KEY_RESULT));
                title = data.get(KEY_TITLE);
                message = data.get(KEY_MESSAGE);
            }

            Episode currEpisode = PatientManager.getMostRecentEpisode(localPatient);

            Date d = PatientManager.getLastReasonOccurrence(localPatient, Episode.REASON_DECEASED, false);
            if (d != null) {
                title = Messages.getString("patient.error.episodeStopReasonInvalid.title"); //$NON-NLS-1$
                message = MessageFormat.format(Messages.getString("patient.error.episodeDuplicateDeceasedReason"), //$NON-NLS-1$
                        localPatient.getPatientId(), iDARTUtil.format(d));
                result = false;
            } else if (currEpisode.getStartReason().equalsIgnoreCase(Episode.REASON_NEW_PATIENT)) {
                d = PatientManager.getLastReasonOccurrence(localPatient, Episode.REASON_NEW_PATIENT, true);
                if (d != null) {
                    title = Messages.getString("patient.error.episodeStartReasonInvalid"); //$NON-NLS-1$
                    message = Messages.getString("patient.error.duplicateNewPatient"); //$NON-NLS-1$
                    result = false;
                }
            }
        }

        if (result) {
            if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade"))
                if (checkOpenmrs) {
                    try {
                        if (getServerStatus(JdbcProperties.urlBase).contains("Red")) {
                            log.trace(new Date() + " :Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador");
                            title = Messages.getString("Servidor OpenMRS offline"); //$NON-NLS-1$
                            message = Messages.getString("Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador"); //$NON-NLS-1$
                            result = true;
                        } else {
                            User currentUser = LocalObjects.getUser(HibernateUtil.getNewSession());

                            assert currentUser != null;
                            if (ApiAuthRest.loginOpenMRS(currentUser)) {

                                restClient = new RestClient();
                                String nidvoided = restClient.getOpenMRSResource(iDartProperties.REST_GET_PERSON_GENERIC + txtOpenmrsuuid.getText());
                                if (nidvoided != null) {

                                    JSONObject jsonObjectPerson = new JSONObject(nidvoided);
                                    Boolean voided = (Boolean) jsonObjectPerson.get("voided");

                                    if (voided) {
                                        title = Messages.getString("UUID Inanctivo no OpenMRS"); //$NON-NLS-1$
                                        message = Messages.getString("O UUID introduzido esta no estado incativo no openmrs, por favor contacte o administrador."); //$NON-NLS-1$
                                        result = false;
                                    }
                                } else {
                                    title = Messages.getString("UUID não existe no OpenMRS"); //$NON-NLS-1$
                                    message = Messages.getString("O UUID introduzido não existe no openmrs, por favor introduza um UUID válido ou contacte o administrador."); //$NON-NLS-1$
                                    result = false;
                                }
                            } else {
                                log.error("O Utilizador " + currentUser.getUsername() + " não se encontra no OpenMRS ou serviço rest no OpenMRS não está  em funcionamento.");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
        }

        if (result) {
            // ARV Start Date test ----------------------------------
            int tabNo = 0;
            // submit data from the tabs
            for (IPatientTab tab : groupTabs) {
                // It should be that if the tab does not validate,
                // then it should not submit... and somehow...
                // it should stop the submit process..
                Map<String, String> map = tab.validateFields(localPatient);
                result = Boolean.valueOf(map.get(KEY_RESULT));
                if (!result) {
                    title = map.get(KEY_TITLE);
                    getLog().info("Tab validation failed - " + title); //$NON-NLS-1$
                    message = map.get(KEY_MESSAGE);
                    tabbedGroup.setSelection(tabNo);
                    tabbedGroup.getTabList()[tabNo].setFocus();
                    break;
                }
                tabNo++;
            }
        }

        // if validation fails show error message
        if (!result) {
            if (message.trim().length() > 0) {
                MessageBox validationError = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                validationError.setText(title);
                validationError.setMessage(message);
                validationError.open();
                // Make sure test fails, on account of tabsOk
                result = false;
            }
        }


        return result;
    }

    /**
     *
     */
    private void editPatientIdentifiers() {
        PatientIdentifierDialog dialog = new PatientIdentifierDialog(getShell(), getHSession(), localPatient);
        dialog.openAndWait();
        identifierChangesMade = dialog.isChangesMade();

        if (identifierChangesMade) {
            boolean checkOpenmrs = true;

            if (CentralizationProperties.centralization.equalsIgnoreCase("off"))
                checkOpenmrs = true;
            else if (CentralizationProperties.pharmacy_type.equalsIgnoreCase("F")
                    || CentralizationProperties.pharmacy_type.equalsIgnoreCase("P"))
                checkOpenmrs = false;


            if ((localPatient.getPatientId() == null || localPatient.getPatientId().isEmpty())) {
                cmdClearWidgetSelected();
            } else {

                txtPatientId.setText(localPatient.getPatientId());

                if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade"))

                    if (checkOpenmrs) {
                        try {
                            if (getServerStatus(JdbcProperties.urlBase).contains("Red")) {
                                log.trace(new Date() + " :Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador");
                                //  showMessage(MessageDialog.WARNING, "Servidor OpenMRS Offline", "Por favor, verifique a conexão com OpenMRS para efectuar esta operação.");
                                return;
                            } else {
                                User currentUser = LocalObjects.getUser(HibernateUtil.getNewSession());

                                assert currentUser != null;
                                if (ApiAuthRest.loginOpenMRS(currentUser)) {

                                    //Preparar Prim.Nomes, Apelido e Data de Nascimento apartir do NID usando REST WEB SERVICES

                                    String nid = txtPatientId.getText().toUpperCase().trim();

                                    String resource = new RestClient().getOpenMRSResource(iDartProperties.REST_GET_PATIENT + StringUtils.replace(nid, " ", "%20"));

                                    String personUuid = (resource.length() > 21) ? resource.substring(21, 57) : "";

                                    String personDemografics = new RestClient().getOpenMRSResource(iDartProperties.REST_GET_PERSON_GENERIC + personUuid);

                                    JSONObject jsonObject = new org.json.JSONObject(personDemografics);

                                    String fullName = jsonObject.getJSONObject("preferredName").getString("display").replace("\r", "").replace("\n", "");

                                    String[] names = fullName.trim().split(" ");

                                    JSONObject locationUuid = null;

                                    String patientEncounterLocation = new RestClient().getOpenMRSResource(iDartProperties.PROGRAM_ENROLLMENT_PATIENT+personUuid+"&v=default");

                                    JSONObject jsonEncounterObject = new org.json.JSONObject(patientEncounterLocation);

                                    JSONArray locationUuidObject = jsonEncounterObject.getJSONArray("results");

                                    try{
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        Date enrolmentDate = dateFormat.parse("1900-01-01");

                                        if(locationUuidObject != null){
                                            for (int i = 0; i < locationUuidObject.length(); i++) {
                                                if (locationUuidObject.getJSONObject(i).get("dateCompleted").toString().equalsIgnoreCase("null")){
                                                    if(enrolmentDate.before(dateFormat.parse(locationUuidObject.getJSONObject(i).getString("dateEnrolled")))){
                                                        enrolmentDate = dateFormat.parse(locationUuidObject.getJSONObject(i).getString("dateEnrolled"));
                                                        locationUuid  = locationUuidObject.getJSONObject(i);
                                                    }
                                                }
                                            }

                                            if(locationUuid != null){
                                                JSONObject uuidObject = locationUuid.getJSONObject("location");

                                                if(uuidObject != null){
                                                    String uuid = uuidObject.getString("uuid");
                                                    localPatient.setUuidlocationopenmrs(uuid);
                                                }
                                            }
                                        }
                                    }catch (Exception e){
                                        localPatient.setUuidlocationopenmrs(JdbcProperties.location);
                                        e.printStackTrace();
                                    }


                                    if(localPatient.getUuidlocationopenmrs() == null)
                                        localPatient.setUuidlocationopenmrs(JdbcProperties.location);

                                    txtFirstNames.setText(fullName.replace(names[names.length - 1], ""));//Primeiros nomes
                                    localPatient.setFirstNames(txtFirstNames.getText());//Primeiros nomes

                                    txtSurname.setText(names[names.length - 1]);//Apelido
                                    localPatient.setLastname(txtSurname.getText());//Apelido

                                    String gender = jsonObject.getString("gender").trim();

                                    if (gender.toUpperCase().startsWith("F")) {
                                        cmbSex.setText(Messages.getString("patient.sex.female")); //$NON-NLS-1$
                                    } else if (gender.toUpperCase().startsWith("M")) {
                                        cmbSex.setText(Messages.getString("patient.sex.male")); //$NON-NLS-1$
                                    }

                                    localPatient.setSex(cmbSex.getText().charAt(0));
                                    txtOpenmrsuuid.setText(localPatient.getUuidopenmrs());

                                    String birthDate = jsonObject.getString("birthdate").trim();

                                    String year = birthDate.substring(0, 4);
                                    String month = new DateFormatSymbols(Locale.ENGLISH).getMonths()[Integer.valueOf(birthDate.substring(5, 7)) - 1];
                                    Integer day = Integer.valueOf(birthDate.substring(8, 10));

                                    SimpleDateFormat sdf = new SimpleDateFormat("d-MMMM-yyyy", Locale.ENGLISH);
                                    theDate = null;//Data de Nascimento
                                    try {
                                        theDate = sdf.parse(day.toString() + "-" + month + "-" + year);
                                    } catch (ParseException e1) {
                                        getLog().error("Error parsing date: ", e1);
                                    }

                                    cmbDOBDay.setText(day.toString());
                                    cmbDOBMonth.setText(month);
                                    cmbDOBYear.setText(year);
                                    localPatient.setDateOfBirth(theDate);
                                } else {
                                    log.error("O Utilizador " + currentUser.getUsername() + " não se encontra no OpenMRS ou serviço rest no OpenMRS não está  em funcionamento.");
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            }
        }
    }

    private void setFormForNewPatient() {
        localPatient = new Patient();
        enableFields(true);
        txtPatientId.setEnabled(false);

        //set other fields
        txtFirstNames.setEnabled(false);
        txtSurname.setEnabled(false);
        cmbSex.setEnabled(false);
        cmbDOBDay.setEnabled(false);
        cmbDOBMonth.setEnabled(false);
        cmbDOBYear.setEnabled(false);

        txtPatientId.setFocus();
        btnEditEpisodes.setEnabled(false);
        btnPatientHistoryReport.setEnabled(false);
    }

    private void cmdSearchWidgetSelected() {
        String patientId = PatientBarcodeParser.getPatientId(txtPatientId.getText());

        if (isAddnotUpdate) {
            if (localPatient == null ||
                    localPatient.getPatientId() == null ||
                    localPatient.getPatientId().isEmpty()) {
                setFormForNewPatient();
                ((TreatmentHistoryTab) groupTabs[3]).enable(true, ResourceUtils
                        .getColor(iDartColor.WIDGET_BACKGROUND)); // ARV
            }

            editPatientIdentifiers();
        } else if (localPatient == null) {
            PatientSearch search = new PatientSearch(getShell(), getHSession());
            search.setShowInactive(true);
            PatientIdentifier identifier = search.search(patientId);

            if (identifier != null) {
                localPatient = identifier.getPatient();
                updateGUIforNewLocalPatient();
            }

            // if we've returned from the search GUI with the user having
            // pressed "cancel", enable the search button
            else if (!btnSearch.isDisposed() & !btnEkapaSearch.isDisposed()) {
                btnSearch.setEnabled(true);
                btnEkapaSearch.setEnabled(true);
            }
            // txtPatientId.setFocus();
        } else {
            editPatientIdentifiers();
        }
    }

    private void cmdSearchWidgetSelectedSearchByName() {
        String patientId = PatientBarcodeParser.getPatientId(txtPatientId.getText());

        isAddnotUpdate = false;

        if (isAddnotUpdate) {
            if (localPatient == null ||
                    localPatient.getPatientId() == null ||
                    localPatient.getPatientId().isEmpty()) {
                setFormForNewPatient();
                ((TreatmentHistoryTab) groupTabs[3]).enable(true, ResourceUtils
                        .getColor(iDartColor.WIDGET_BACKGROUND)); // ARV
            }

            editPatientIdentifiers();
        } else if (localPatient == null) {
            PatientSearch search = new PatientSearch(getShell(), getHSession());
            search.setShowInactive(true);
            PatientIdentifier identifier = search.search(patientId);

            if (identifier != null) {
                localPatient = identifier.getPatient();
                updateGUIforNewLocalPatient();
            }

            // if we've returned from the search GUI with the user having
            // pressed "cancel", enable the search button
            else if (!btnSearch.isDisposed() & !btnEkapaSearch.isDisposed()) {
                btnSearch.setEnabled(true);
                btnEkapaSearch.setEnabled(true);
            }
            // txtPatientId.setFocus();
        } else {
            editPatientIdentifiers();
        }

    }

    private void updateGUIforNewLocalPatient() {
        boolean checkOpenmrs = true;
        if (CentralizationProperties.centralization.equalsIgnoreCase("on") && (CentralizationProperties.pharmacy_type.equalsIgnoreCase("F")
                || CentralizationProperties.pharmacy_type.equalsIgnoreCase("P")))
            checkOpenmrs = false;

        loadPatientDetails();
        txtPatientId.setEnabled(false);
        txtFirstNames.setFocus();

        btnSearch.setText(Messages.getString("patient.button.editid")); //$NON-NLS-1$
        btnSearch.setToolTipText(Messages.getString("patient.button.editid.tooltip")); //$NON-NLS-1$
        btnEkapaSearch.setEnabled(false);

        // Check if patient has alternate ids
        if (localPatient.getAlternateIdentifiers().size() > 0) {
            alternativePatientIds = true;
        } else {
            alternativePatientIds = false;
        }
        enableFields(true);

        if (localPatient.getMostRecentEpisode() != null)
            if (checkOpenmrs && !(localPatient.getMostRecentEpisode().getStartReason().contains("nsito") ||
                    localPatient.getMostRecentEpisode().getStartReason().contains("nidade"))) {
                txtOpenmrsuuid.setVisible(true);
                lblOpenmrsuuid.setVisible(true);
            }


    }

    /**
     * Method submitForm.
     *
     * @return boolean
     */
    private boolean confirmSave() {
		/*MessageBox mSave = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES	| SWT.NO);
		mSave.setText(isAddnotUpdate ? Messages.getString("patient.dialog.addNewPatient.title") //$NON-NLS-1$
				: Messages.getString("patient.dialog.upatePatient.title")); //$NON-NLS-1$
		mSave.setMessage(isAddnotUpdate ? Messages.getString("patient.dialog.addNewPatient") //$NON-NLS-1$
				: Messages.getString("patient.dialog.upatePatient")); //$NON-NLS-1$

		return mSave.open() == SWT.YES;*/

        MessageDialog dialog = new MessageDialog(getShell(), isAddnotUpdate ? Messages.getString("patient.dialog.addNewPatient.title")
                : Messages.getString("patient.dialog.upatePatient.title"), null, isAddnotUpdate ? Messages.getString("patient.dialog.addNewPatient") //$NON-NLS-1$
                : Messages.getString("patient.dialog.upatePatient"), MessageDialog.QUESTION, new String[]{"Sim", "Não"}, 0);

        return dialog.open() == 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.celllife.idart.gui.platform.GenericFormGui#submitForm()
     */
    @Override
    protected boolean submitForm() {
        if (localPatient.getSex() == 'u' || localPatient.getSex() == 'U') {
            MessageBox
                    m = new MessageBox(getShell(), SWT.YES | SWT.NO
                    | SWT.ICON_QUESTION);
            m.setText("Patient Gender set to Unknown");
            m
                    .setMessage("Are you sure you want to save this patient's gender as unknown?");
            if (m.open() == SWT.NO) {
                cmbSex.setFocus();
                return false;
            }
        }

        Transaction tx = null;

        try {

            tx = getHSession().beginTransaction();

            for (IPatientTab tab : groupTabs) {
                tab.submit(localPatient);
            }

            PatientManager.savePatient(getHSession(), localPatient);

            log.trace(" local patient " + localPatient.getPatientId() + "  " + localPatient.getFirstNames());

            //ConexaoODBC conn=new ConexaoODBC();
            ConexaoJDBC conn2 = new ConexaoJDBC();

            //insere pacientes no idart
            try {
                try {
                    if (isAddnotUpdate)
                        conn2.inserPacienteIdart(localPatient.getPatientId(), localPatient.getFirstNames(), localPatient.getLastname(), new Date());
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            getHSession().flush();
            tx.commit();

            MessageBox m = new MessageBox(getShell(), SWT.OK | SWT.ICON_INFORMATION);
            m.setText(Messages.getString("patient.save.confirmation.title")); //$NON-NLS-1$
            m.setMessage(MessageFormat.format(Messages.getString("patient.save.confirmation"), localPatient.getPatientId())); //$NON-NLS-1$
            m.open();

            if (isAddnotUpdate || offerToPrintLabel) {
                m = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                m.setText(Messages.getString("patient.dialog.printInfoLable.title")); //$NON-NLS-1$
                m.setMessage(Messages.getString("patient.dialog.printInfoLable")); //$NON-NLS-1$
                switch (m.open()) {
                    case SWT.YES:
                        printPatientInfoLabel();
                        break;
                }
            }

            return true;

        } catch (HibernateException he) {
            if (tx != null) {
                tx.rollback();
            }
            getLog().error("Error saving patient to the database.", he); //$NON-NLS-1$
            MessageBox m = new MessageBox(getShell(), SWT.OK | SWT.ICON_INFORMATION);
            m.setText(Messages.getString("patient.error.save.failed.title")); //$NON-NLS-1$
            m.setMessage(Messages.getString("patient.error.save.failed")); //$NON-NLS-1$
            m.open();

            return false;
        }
    }

    private void setLocalPatient() {
        boolean checkOpenmrs = true;

        if (CentralizationProperties.centralization.equalsIgnoreCase("off"))
            checkOpenmrs = true;
        else if (CentralizationProperties.pharmacy_type.equalsIgnoreCase("F")
                || CentralizationProperties.pharmacy_type.equalsIgnoreCase("P"))
            checkOpenmrs = false;

        // Bug iDART-86
        localPatient.setLastname(txtSurname.getText());
        localPatient.setFirstNames(txtFirstNames.getText());


        localPatient.setModified('T');
        localPatient.setPatientId(txtPatientId.getText().toUpperCase());//NID
        localPatient.setCellphone(txtCellphone.getText().trim());


        if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade"))
            if (checkOpenmrs && isAddnotUpdate) {
                try {
                    if (getServerStatus(JdbcProperties.urlBase).contains("Red")) {
                        log.trace(new Date() + " :Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador");
                        showMessage(MessageDialog.WARNING, "Servidor OpenMRS Offline", "Por favor, verifique a conexão com OpenMRS para efectuar esta operação.");
                        return;
                    } else {
                        User currentUser = LocalObjects.getUser(HibernateUtil.getNewSession());

                        assert currentUser != null;
                        if (ApiAuthRest.loginOpenMRS(currentUser)) {

                            if (localPatient.getUuidopenmrs() == null) {
                                String openMrsResource = new RestClient().getOpenMRSResource("patient?q=" + StringUtils.replace(txtPatientId.getText().trim(), " ", "%20"));

                                JSONObject _jsonObject = new JSONObject(openMrsResource);

                                String personUuid = null;

                                JSONArray _jsonArray = (JSONArray) _jsonObject.get("results");

                                for (int i = 0; i < _jsonArray.length(); i++) {
                                    JSONObject results = (JSONObject) _jsonArray.get(i);
                                    personUuid = (String) results.get("uuid");
                                }
                                localPatient.setUuidopenmrs(personUuid);
                            }
                        } else {
                            log.error("O Utilizador " + currentUser.getUsername() + " não se encontra no OpenMRS ou serviço rest no OpenMRS não está  em funcionamento.");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        if (cmbSex.getText().toUpperCase().startsWith("F")) {
            localPatient.setSex(Messages.getString("patient.sex.female").charAt(0)); //$NON-NLS-1$
        } else if (cmbSex.getText().toUpperCase().startsWith("M")) {
            localPatient.setSex(Messages.getString("patient.sex.male").charAt(0)); //$NON-NLS-1$
        }

        // Set the date of birth  Bug iDART-86
        SimpleDateFormat sdf = new SimpleDateFormat("d-MMMM-yyyy", Locale.ENGLISH); //$NON-NLS-1$
        Date theDate = null;//Data de Nascimento
        try {
            theDate = sdf.parse(cmbDOBDay.getText() + "-" //$NON-NLS-1$
                    + cmbDOBMonth.getText() + "-" + cmbDOBYear.getText()); //$NON-NLS-1$
        } catch (ParseException e1) {
            getLog().error("Error parsing date: ", e1); //$NON-NLS-1$
        }

        localPatient.setDateOfBirth(theDate);

        Date episodeStartDate = btnEpisodeStartDate.getDate();
        Date episodeStopDate = btnEpisodeStopDate.getDate();

        if (isPatientActive) {
            Episode e = PatientManager.getMostRecentEpisode(localPatient);
            if (episodeStartDate != null) {
                e.setStartDate(episodeStartDate);
                e.setStartReason(cmbEpisodeStartReason.getText());
                e.setStartNotes(txtEpisodeStartNotes.getText());
                e.setClinic(AdministrationManager.getClinic(getHSession(), cmbClinic.getText()));
                e.setStopDate(episodeStopDate);
                e.setStopReason(cmbEpisodeStopReason.getText());
                e.setStopNotes(txtEpisodeStopNotes.getText());
            }
            localPatient.setAccountStatus(episodeStopDate == null);
        } else if (!cmbEpisodeStartReason.getText().trim().isEmpty()) {
            // If patient is not active, this is then an opening episode.
            // This episode that is created here is to be added to the patient,
            // this is the patient's new episode.
            Episode e = new Episode();
            e.setStartDate(episodeStartDate);
            e.setStartReason(cmbEpisodeStartReason.getText());
            e.setStartNotes(txtEpisodeStartNotes.getText());
            e.setClinic(AdministrationManager.getClinic(getHSession(),
                    cmbClinic.getText()));
            e.setStopDate(episodeStopDate);
            e.setStopReason(cmbEpisodeStopReason.getText());
            e.setStopNotes(txtEpisodeStopNotes.getText());
            PatientManager.addEpisodeToPatient(localPatient, e);
            localPatient.setAccountStatus((episodeStopDate == null));
        }

        if (localPatient.getMostRecentEpisode() != null) {
            if (localPatient.getMostRecentEpisode().getStartReason().contains("Voltou da Refer")) {

                List<SyncEpisode> syncEpisodeList = EpisodeManager.getAllSyncTempEpiReadyToSendForPacient(getHSession(), localPatient);

                if (syncEpisodeList.isEmpty()) {
                    SyncEpisode syncEpisode = SyncEpisode.generateFromEpisode(localPatient.getMostRecentEpisode(), localPatient.getCurrentClinic(), AdministrationManager.getMainClinic(getHSession()).getUuid());
                    EpisodeManager.saveSyncTempEpisode(syncEpisode);
                } else {
                    MessageBox missing = new MessageBox(getShell(), SWT.ICON_ERROR
                            | SWT.OK);
                    missing.setText("Este paciente ja voltou da referência.");
                    missing
                            .setMessage("Ja existe um episódio de volta registado para este paciente na mesma Farmacia que não foi enviada.");
                    missing.open();
                }
            }
        }

        if (btnARVStart.getDate() != null) {
            localPatient.setAttributeValue(
                    PatientAttribute.ARV_START_DATE, btnARVStart.getDate());
        }

        localPatient.updateClinic();

        if (checkOpenmrs) {
            if (!isAddnotUpdate)
                localPatient.setUuidopenmrs(txtOpenmrsuuid.getText());
            // update the patient with details from the tabs
            for (IPatientTab tab : groupTabs) {
                tab.setPatientDetails(localPatient);
            }
        }
    }

    @Override
    protected void cmdCancelWidgetSelected() {
        closeShell(true);
    }

    @Override
    protected void cmdClearWidgetSelected() {
        btnEpisodeStartDate.clearDate();
        btnEpisodeStopDate.clearDate();
        isPatientActive = false;
        localPatient = null;
        clearForm();
        enableFields(false);
        txtPatientId.setEnabled(!isAddnotUpdate);
        txtPatientId.setFocus();
        if (isAddnotUpdate) {
            btnSearch.setText(Messages.getString("patient.button.editid")); //$NON-NLS-1$
            btnSearch
                    .setToolTipText(Messages.getString("patient.button.editid.tooltip")); //$NON-NLS-1$
        } else {
            btnSearch.setText(Messages.getString("patient.button.search")); //$NON-NLS-1$
            btnSearch
                    .setToolTipText(Messages.getString("patient.button.search.tooltip")); //$NON-NLS-1$
        }
        btnSearch.setEnabled(true);
        btnEkapaSearch.setEnabled(true);
        lblPicChild.setVisible(false);
    }

    /**
     * Method cmdUpdatePrescriptionWidgetSelected.
     */
    private void cmdUpdatePrescriptionWidgetSelected() {
        boolean proceed = true;
        if (isSaveRequired()) {
            proceed = doSave();
        }
        if (proceed) {
            if (localPatient.getId() != -1) {

                for (PatientIdentifier identifier : localPatient.getPatientIdentifiers()) {
                    if (identifier.getType().isNID()) {
                        new AddPrescription(localPatient, getParent(), false, iDartProperties.SERVICOTARV);
                        break;
                    }
                }

                for (PatientIdentifier identifier : localPatient.getPatientIdentifiers()) {
                    if (identifier.getType().isPREP()) {
                        new AddPrescription(localPatient, getParent(), false, iDartProperties.PREP);
                        break;
                    }
                }

                for (PatientIdentifier identifier : localPatient.getPatientIdentifiers()) {
                    if (identifier.getType().getName().contains("CCR")) {
                        new AddPrescription(localPatient, getParent(), false, iDartProperties.PNCT);
                        break;
                    }
                }

                // myPrescription.addDisposeListener(new DisposeListener() {
                // public void widgetDisposed(DisposeEvent e1) {
                cmdCancelWidgetSelected();
                // }
                // });
            }
        }

    }

    /**
     * Method cmdPrintPatientLabelSelected.
     */
    private void cmdPrintPatientLabelSelected() {
        printPatientInfoLabel();
    }

    /**
     * This method loads the details of the patient into the GUI. It used the
     * localPatient object to get the values
     */
    public void loadPatientDetails() {

        try {
            // run this first to set isPatientActive
            loadEpisodeDetails();

            // populate the GUI
            txtFirstNames.setText(localPatient.getFirstNames());
            txtSurname.setText(localPatient.getLastname());
            if (localPatient.getUuidopenmrs() != null) {
                txtOpenmrsuuid.setText(localPatient.getUuidopenmrs());
            }
            char sex = localPatient.getSex();
            if (Character.toUpperCase(sex) == 'F') {
                cmbSex.setText(Messages.getString("patient.sex.female")); //$NON-NLS-1$
            } else if (Character.toUpperCase(sex) == 'M') {
                cmbSex.setText(Messages.getString("patient.sex.male")); //$NON-NLS-1$
            }

            txtAge.setText(String.valueOf(localPatient.getAge()));

            txtCellphone.setText(localPatient.getCellphone());
            originalPatientId = localPatient.getPatientId();
            txtPatientId.setText(originalPatientId);
            Calendar theDOB = Calendar.getInstance();
            theDOB.setTime(localPatient.getDateOfBirth());
            cmbDOBDay.setText(String.valueOf(theDOB.get(Calendar.DAY_OF_MONTH)));
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH); //$NON-NLS-1$
            cmbDOBMonth.setText(monthFormat.format(theDOB.getTime()));
            cmbDOBYear.setText(String.valueOf(theDOB.get(Calendar.YEAR)));
            if (localPatient.getAge() <= 12) {
                lblPicChild.setVisible(true);
            }
            // Enable the prescription button
            btnUpdatePrescription.setEnabled(true);
            epViewer = new EpisodeViewer(getHSession(), getShell(), localPatient, true);
            epViewer.addChangeListener(this);

            Episode mostRecentEpisode = localPatient.getMostRecentEpisode();
            if (btnDownRefer != null
                    && localPatient.getCurrentClinic().isMainClinic()
                    && (mostRecentEpisode != null && mostRecentEpisode.isOpen())) {
                btnDownRefer.setEnabled(true);
            } else {
                btnDownRefer.setEnabled(false);
            }

            // Setting ARV Start Date
            PatientAttribute pa = localPatient.getAttributeByName(PatientAttribute.ARV_START_DATE);
            if (pa != null) {
                try {
                    Date arvStartDate = (Date) pa.getObjectValue();
                    btnARVStart.setDate((Date) arvStartDate.clone());
                } catch (Exception e) {
                    getLog().error("Error when obtaining ARV Start Date", e); //$NON-NLS-1$
                }
            }

            // load the tabs with data from the patient
            for (IPatientTab tab : groupTabs) {
                tab.loadPatientDetails(localPatient, isPatientActive);
            }

        } catch (Exception e) {
            getLog().error(e);
        }
    }

    public void loadEpisodeDetails() {

        // episodes
        List<Episode> episodes = localPatient.getEpisodeList();
        Episode mostRecentEpisode = PatientManager.getMostRecentEpisode(localPatient);
        isPatientActive = mostRecentEpisode.isOpen();

        if (isPatientActive) {

            if (isAddnotUpdate) {
                lblEpisodeTitle.setText(Messages.getString("patient.label.episode.new")); //$NON-NLS-1$
            } else {
                lblEpisodeTitle.setText(Messages.getString("patient.label.episode.current")); //$NON-NLS-1$
            }

            cmbEpisodeStartReason.setText(mostRecentEpisode.getStartReason());
            Date date = mostRecentEpisode.getStartDate();
            if (date != null) {
                btnEpisodeStartDate.setDate(date);
            }
            txtEpisodeStartNotes.setText(mostRecentEpisode.getStartNotes());

            cmbEpisodeStopReason.setText(mostRecentEpisode.getStopReason());
            date = mostRecentEpisode.getStopDate();
            if (date != null) {
                btnEpisodeStopDate.setDate(date);
            }
            txtEpisodeStopNotes.setText(mostRecentEpisode.getStopNotes());

            cmbClinic.setText(mostRecentEpisode.getClinic().getClinicName());

            if (episodes.size() > 1) {
                lblPastEpisodeStart.setText(episodes.get(episodes.size() - 2)
                        .getStartReason()
                        + " (" //$NON-NLS-1$
                        + iDARTUtil.format(episodes.get(episodes.size() - 2)
                        .getStartDate()) + ")"); //$NON-NLS-1$
                lblPastEpisodeStop.setText(episodes.get(episodes.size() - 2)
                        .getStopReason()
                        + " (" //$NON-NLS-1$
                        + iDARTUtil.format(episodes.get(episodes.size() - 2)
                        .getStopDate()) + ")"); //$NON-NLS-1$
                lblPastEpisodeDivide.setVisible(true);
                btnEditEpisodes.setEnabled(true);
            } else {
                btnEditEpisodes.setEnabled(false);
            }
        } else if (episodes.size() >= 1) {
            // This is the episode information for a patient who is
            // inactive. Here we display that he does NOT have an
            // episode.
            lblEpisodeTitle
                    .setText(Messages.getString("patient.error.episode.missing")); //$NON-NLS-1$
            lblEpisodeTitle.setForeground(ResourceUtils
                    .getColor(iDartColor.RED));

            lblPastEpisodeStart.setText(mostRecentEpisode.getStartReason()
                    + " (" + iDARTUtil.format(mostRecentEpisode.getStartDate()) //$NON-NLS-1$
                    + ")"); //$NON-NLS-1$
            lblPastEpisodeStop.setText(mostRecentEpisode.getStopReason() + " (" //$NON-NLS-1$
                    + iDARTUtil.format(mostRecentEpisode.getStopDate()) + ")"); //$NON-NLS-1$
            lblPastEpisodeDivide.setVisible(true);
            btnEditEpisodes.setEnabled(true);
        } else {
            cmbEpisodeStartReason.setText(Episode.REASON_NEW_PATIENT);
            btnEpisodeStartDate.setDate(new Date());
            btnEditEpisodes.setEnabled(false);
        }

    }

    /**
     * checks if the given date is valid
     *
     * @param strDay   String
     * @param strMonth String
     * @param strYear  String
     * @return true if the date is valid else false
     */
    public boolean dateOkay(String strDay, String strMonth, String strYear) {

        boolean result = false;

        try {

            int day = Integer.parseInt(strDay);

            // check the year
            if (strYear.length() != 4)
                return result;
            int year = Integer.parseInt(strYear);

            // get the int value for the string month (e.g. January)
            // int month = Integer.parseInt(strMonth);
            int month = -1;
            for (int i = 0; i < cmbDOBMonth.getItemCount(); i++) {
                if (strMonth.equals(cmbDOBMonth.getItem(i))) {
                    month = i;
                }
            }

            switch (month) {
                case -1:
                    result = false;
                    break;
                case Calendar.FEBRUARY:
                    if (day <= 29) {
                        GregorianCalendar greg = new GregorianCalendar();
                        if (day == 29 & greg.isLeapYear(year)) {
                            result = true;
                        } else {
                            if (day == 29) {
                                result = false;
                            } else {
                                result = true;
                            }
                        }
                    } else {
                        result = false;
                    }
                    break;
                case Calendar.JANUARY | Calendar.MARCH | Calendar.MAY
                        | Calendar.JULY | Calendar.AUGUST | Calendar.OCTOBER
                        | Calendar.DECEMBER:
                    if (day <= 31) {
                        result = true;
                    } else {
                        result = false;
                    }
                    break;
                default:
                    result = true;
                    break;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return result;

    }

    /**
     * Method enableFields.
     *
     * @param enable boolean
     */
    @Override
    protected void enableFields(boolean enable) {
        // set the colour of each of the Combo's
        Color myColour = null;
        if (enable) {
            myColour = ResourceUtils.getColor(iDartColor.WHITE);
        } else {
            myColour = ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND);
        }
        // Patient Particulars fields
        txtFirstNames.setEnabled(enable);
        txtSurname.setEnabled(enable);
        btnPatientHistoryReport.setEnabled(enable);

        if (alternativePatientIds && enable) {
            lblOtherPatientsWithThisID.setVisible(true);
        } else {
            lblOtherPatientsWithThisID.setVisible(false);
        }
        cmbDOBDay.setEnabled(enable);
        cmbDOBMonth.setEnabled(enable);
        cmbDOBYear.setEnabled(enable);
        txtCellphone.setEnabled(enable);
        cmbSex.setEnabled(enable);
        // cmbRace.setEnabled(enable);

        // Account Information fields
        Episode e = localPatient != null ? localPatient.getMostRecentEpisode() : null;
        cmbClinic.setEnabled(e != null && e.isOpen());
        cmbClinic.setBackground(myColour);

        if (!iDARTUtil.arrayHasElements(LocalObjects.getUser(getHSession()).getRoleSet()) || LocalObjects.getUser(getHSession()).hasRole(Role.PHARMACIST)) {
            btnUpdatePrescription.setEnabled(enable);
            lblPicUpdatePrescription.setEnabled(enable);
        }

        if (!isAddnotUpdate) {
            btnPrintPatientLabel.setEnabled(enable);
            txtOpenmrsuuid.setEnabled(enable);
        }
        lblPicPrintPatientLabel.setEnabled(enable);

        btnEkapaSearch.setEnabled(!enable);
        btnSave.setEnabled(enable);

        cmbDOBDay.setBackground(myColour);
        cmbDOBMonth.setBackground(myColour);
        cmbDOBYear.setBackground(myColour);
        cmbSex.setBackground(myColour);
        // cmbRace.setBackground(myColour);

        cmbEpisodeStartReason.setBackground(myColour);
        btnEpisodeStartDate.setBackground(myColour);
        txtEpisodeStartNotes.setBackground(myColour);
        cmbEpisodeStopReason.setBackground(myColour);
        btnEpisodeStopDate.setBackground(myColour);
        txtEpisodeStopNotes.setBackground(myColour);

        if (enable) {
            setEpisodeFields();
        } else {
            cmbEpisodeStartReason.setText(EMPTY);
            cmbEpisodeStopReason.setText(EMPTY);

            cmbEpisodeStartReason.setEnabled(false);
            btnEpisodeStartDate.setEnabled(false);
            txtEpisodeStartNotes.setEnabled(false);

            cmbEpisodeStopReason.setEnabled(false);
            btnEpisodeStopDate.setEnabled(false);
            txtEpisodeStopNotes.setEnabled(false);

            btnEditEpisodes.setEnabled(false);
        }

        if (btnDownRefer != null && isAddnotUpdate) {
            btnDownRefer.setEnabled(false);
        }

        btnARVStart.setEnabled(enable);

        // enable the tabs
        for (IPatientTab tab : groupTabs) {
            tab.enable(enable, myColour);
        }
        if (enable && cmbSex.getSelectionIndex() == 0) {
            // The combo box displays a female,
            // and so the clinic info tab has to be enabled
            // at start up.
            ((ClinicInfoTab) groupTabs[2]).setPatientIsFemale(true);
            groupTabs[2].enable(true, null);
        }

    }

    private void setEpisodeFields() {

        cmbEpisodeStartReason.setEnabled(true);
        btnEpisodeStartDate.setEnabled(true);
        txtEpisodeStartNotes.setEnabled(true);

        cmbEpisodeStopReason.setEnabled(isPatientActive);
        btnEpisodeStopDate.setEnabled(isPatientActive);
        txtEpisodeStopNotes.setEnabled(isPatientActive);

        if (!isPatientActive) {
            cmbEpisodeStopReason.setBackground(ResourceUtils
                    .getColor(iDartColor.WIDGET_BACKGROUND));
            btnEpisodeStopDate.setBackground(ResourceUtils
                    .getColor(iDartColor.WIDGET_BACKGROUND));
            txtEpisodeStopNotes.setBackground(ResourceUtils
                    .getColor(iDartColor.WIDGET_BACKGROUND));
        }
        lblPastEpisodeTitle.setText(Messages.getString("patient.label.episode.previous")); //$NON-NLS-1$

        if (isAddnotUpdate) {
            btnEpisodeStartDate.setDate(new Date());
            cmbEpisodeStartReason.setText(Episode.REASON_NEW_PATIENT);
            lblPastEpisodeTitle.setText(Messages.getString("patient.label.noPreviousEpisodes")); //$NON-NLS-1$
        } else if (PatientManager.hasPreviousEpisodes(localPatient)) {
            btnEditEpisodes.setEnabled(true);
            lblPastEpisodeTitle.setForeground(ResourceUtils
                    .getColor(iDartColor.BLACK));
            lblPastEpisodeTitle.setText(MessageFormat.format(Messages
                            .getString("patient.label.previousEpisodes"), //$NON-NLS-1$
                    PatientManager.getNoOfEpisodes(localPatient)));
        }
    }

    /**
     * This method is called whenever an update needs to be made to the
     * database. The idea behind it is to reduce unnecessary database updates.
     *
     * @return true if changes have been made to this patient, false otherwise
     */
    private boolean changesMadeToPatient() {

        if (identifierChangesMade) {
            offerToPrintLabel = true;
            return true;
        }

        if (labelPrintChangesMade()) {
            offerToPrintLabel = true;
            return true;
        }

        if (localPatient.getUuidopenmrs() == null) {
            return true;
        }

        if (!localPatient.getCellphone().trim().equals(
                txtCellphone.getText().trim()))
            return true;

        Episode mostRecentEpisode = PatientManager
                .getMostRecentEpisode(localPatient);

        if (mostRecentEpisode.isOpen()) {
            if (!mostRecentEpisode.getStartReason().trim().equalsIgnoreCase(
                    cmbEpisodeStartReason.getText().trim()))
                return true;
            // have the starting attributes of the current episode been changed?
            Date date = mostRecentEpisode.getStartDate();
            if ((date == null && btnEpisodeStartDate.getDate() == null)
                    || (DateFieldComparator.compare(date, btnEpisodeStartDate
                    .getDate(), Calendar.DAY_OF_MONTH) != 0))
                return true;

            if (!mostRecentEpisode.getStartNotes().trim().equalsIgnoreCase(
                    txtEpisodeStartNotes.getText().trim()))
                return true;

            if (!mostRecentEpisode.getClinic().getClinicName().trim()
                    .equalsIgnoreCase(cmbClinic.getText().trim()))
                return true;

            // has the episode been ended?
            if (!cmbEpisodeStopReason.getText().trim().isEmpty())
                return true;
            if (!txtEpisodeStopNotes.getText().trim().isEmpty())
                return true;
            if (!btnEpisodeStopDate.getText().trim().equalsIgnoreCase(
                    Messages.getString("patient.label.stopdate"))) //$NON-NLS-1$
                return true;

        } else {
            // has a new episode been started?
            if (btnEpisodeStartDate.getDate() != null)
                return true;
            else if (!cmbEpisodeStartReason.getText().trim().isEmpty())
                return true;
            else if (!txtEpisodeStartNotes.getText().trim().isEmpty())
                return true;
        }

        if (!epViewer.changesMade) {
            // submit data from the tabs
            for (IPatientTab tab : groupTabs) {
                if (tab.changesMade(localPatient))
                    return true;
            }
        }

        PatientAttribute pa = localPatient.getAttributeByName(PatientAttribute.ARV_START_DATE);
        if (btnARVStart.getDate() != null) {
            if (pa != null) {
                if (btnARVStart.getDate().compareTo((Date) pa.getObjectValue()) != 0)
                    return true;
            } else
                return true;
        }
        return false;
    }

    /**
     * Examine the patient details and determine whether a new patient info
     * label should be printed.
     *
     * @return true if changes have been made to any attribute that appears on
     * the patient info label or the patient is a new patient
     */
    private boolean labelPrintChangesMade() {

        if (isAddnotUpdate)
            return true;

        // get the DOB on the GUI into a format that can be compared with the
        // DOB from the database
        String theNewDate = EMPTY;
        String theNewMonth = EMPTY;
        theNewMonth += cmbDOBMonth.getText().substring(0, 3);

        if (cmbDOBDay.getText().length() == 1) {
            theNewDate = "0"; //$NON-NLS-1$
        }
        theNewDate += cmbDOBDay.getText() + " " + theNewMonth + " " + cmbDOBYear.getText(); //$NON-NLS-1$ //$NON-NLS-2$

        if (!theNewDate.equals(iDARTUtil.format(localPatient.getDateOfBirth())))
            return true;
        if (!(localPatient.getPatientId().trim().toUpperCase().equals(txtPatientId.getText().trim().toUpperCase())))
            return true;
        if (!(localPatient.getFirstNames().trim().toUpperCase().equals(txtFirstNames.getText().trim().toUpperCase())))
            return true;
        if (!(localPatient.getLastname().trim().toUpperCase().equals(txtSurname.getText().trim().toUpperCase())))
            return true;
        if (localPatient.getUuidopenmrs() != null && !(localPatient.getUuidopenmrs().trim().equals(txtOpenmrsuuid.getText().trim())))
            return true;
        if (!(Character.toUpperCase(localPatient.getSex()) == Character.toUpperCase(cmbSex.getText().charAt(0))))
            return true;

        return false;
    }

    /**
     * This method initializes compUpdatePrescription
     */
    private void createCompUpdatePrescription() {
        compUpdatePrescription = new Composite(getShell(), SWT.NONE);
        compUpdatePrescription.setBounds(new Rectangle(151, 560, 605, 50));

        boolean enableUpdatePrescrition = (!iDARTUtil.arrayHasElements(LocalObjects.getUser(getHSession()).getRoleSet()) || LocalObjects.getUser(getHSession()).hasRole(Role.PHARMACIST));

        lblPicUpdatePrescription = new Label(compUpdatePrescription, SWT.NONE);
        lblPicUpdatePrescription.setBounds(new Rectangle(16, 3, 50, 43));
        lblPicUpdatePrescription.setImage(ResourceUtils
                .getImage(iDartImage.PRESCRIPTIONNEW));
        lblPicUpdatePrescription.setVisible(enableUpdatePrescrition);

        btnUpdatePrescription = new Button(compUpdatePrescription, SWT.NONE);
        btnUpdatePrescription.setBounds(new Rectangle(70, 10, 220, 30));
        btnUpdatePrescription.setFont(ResourceUtils
                .getFont(iDartFont.VERASANS_8));
        btnUpdatePrescription
                .setText(isAddnotUpdate ? Messages.getString("patient.button.prescription.create") //$NON-NLS-1$
                        : Messages.getString("patient.button.prescription.update")); //$NON-NLS-1$
        btnUpdatePrescription
                .setToolTipText(Messages.getString("patient.button.prescription.update.tooltip")); //$NON-NLS-1$
        btnUpdatePrescription
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            SelectionEvent e) {

                        if (PatientManager.getMostRecentEpisode(localPatient)
                                .isOpen()
                                || localPatient.getId() == -1
                                || !cmbEpisodeStartReason.getText().isEmpty()) {
                            cmdUpdatePrescriptionWidgetSelected();
                        } else {
                            MessageBox msgbox = new MessageBox(getShell(),
                                    SWT.ICON_WARNING | SWT.OK
                                            | SWT.PRIMARY_MODAL);
                            msgbox.setText(Messages.getString("patient.error.noOpenEpisode.title")); //$NON-NLS-1$
                            msgbox.setMessage(Messages.getString("patient.error.noOpenEpisode")); //$NON-NLS-1$
                            msgbox.open();
                        }
                    }
                });
        btnUpdatePrescription.setVisible(enableUpdatePrescrition);

        lblPicPrintPatientLabel = new Label(compUpdatePrescription, SWT.NONE);
        lblPicPrintPatientLabel.setBounds(new Rectangle(316, 3, 50, 43));
        lblPicPrintPatientLabel.setImage(ResourceUtils
                .getImage(iDartImage.PATIENTINFOLABEL));

        btnPrintPatientLabel = new Button(compUpdatePrescription, SWT.NONE);
        btnPrintPatientLabel.setBounds(new Rectangle(370, 10, 220, 30));
        btnPrintPatientLabel.setFont(ResourceUtils
                .getFont(iDartFont.VERASANS_8));
        btnPrintPatientLabel.setText(Messages.getString("patient.button.printInfoLabel")); //$NON-NLS-1$
        btnPrintPatientLabel
                .setToolTipText(Messages.getString("patient.button.printInfoLabel.tooltip")); //$NON-NLS-1$
        btnPrintPatientLabel
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        cmdPrintPatientLabelSelected();
                    }
                });

        if (isAddnotUpdate) {
            btnPrintPatientLabel.setEnabled(false);
        }
    }

    public void cmdUpdateAge() {

        SimpleDateFormat sdf = new SimpleDateFormat("d-MMMM-yyyy", Locale.ENGLISH); //$NON-NLS-1$
        try {
            // Set the date of birth
            if ((!cmbDOBDay.getText().isEmpty())
                    && (!cmbDOBMonth.getText().isEmpty())
                    && (!cmbDOBYear.getText().isEmpty())) {
                Date theDate = sdf.parse(cmbDOBDay.getText() + "-" //$NON-NLS-1$
                        + cmbDOBMonth.getText() + "-" + cmbDOBYear.getText()); //$NON-NLS-1$

                Calendar today = Calendar.getInstance();
                Calendar dob = Calendar.getInstance();
                dob.setTime(theDate);
                // Get age based on year
                int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                // Add the tentative age to the date of birth to get this year's
                // birthday
                dob.add(Calendar.YEAR, age);
                // If birthday hasn't happened yet, subtract one from
                // age
                if (today.before(dob)) {
                    age--;
                }
                txtAge.setText(String.valueOf(age));
            }
        } catch (ParseException nbe) {

        }
    }

    /**
     * Method patIdLengthOk.
     *
     * @return boolean
     */
    private boolean patIdLengthOk() {
        Barcode barC = new Barcode("010203A-" + txtPatientId.getText() + "-1"); //$NON-NLS-1$ //$NON-NLS-2$
        if (barC.getBarcodeLengthInChars() > Barcode.getLengthForCurrentOS())
            return false;
        return true;
    }

    private void printPatientInfoLabel() {
        // set up a patient info label
        Object myInfoLabel;
        myInfoLabel = new PatientInfoLabel(localPatient);
        ArrayList<Object> labelList = new ArrayList<Object>(1);

        labelList.add(myInfoLabel);

        try {
            PrintLabel.printiDARTLabels(labelList);
        } catch (Exception e) {
            getLog().error("Error printing patient info label", e); //$NON-NLS-1$
        }
    }

    private void cmdPatientHistoryWidgetSelected() {

        getLog().info("Reports Page: User chose 'Patient History Report'"); //$NON-NLS-1$

        if (localPatient != null) {
            PatientHistoryReport report = new PatientHistoryReport(getShell(),
                    localPatient, PatientHistoryReport.PATIENT_HISTORY_FILA);
            viewReport(report);
        } else {
            PatientHistory patHistory = new PatientHistory(getShell(), true);
            patHistory.openShell();
        }
    }

    private void createCmpTabbedInfo() {
        cmpTabbedGroup = new Composite(compPatientInfo, SWT.NONE);
        cmpTabbedGroup.setBounds(new Rectangle(30, 300, 790, 205));
        cmpTabbedGroup.setBackground(ResourceUtils
                .getColor(iDartColor.WIDGET_BACKGROUND));
        tabbedGroup = new TabFolder(cmpTabbedGroup, SWT.NONE);
        tabbedGroup.setBounds(new Rectangle(0, 0, 785, 205));
        tabbedGroup.setBackground(ResourceUtils
                .getColor(iDartColor.WIDGET_BACKGROUND));
        tabbedGroup.setFont(ResourceUtils.getFont(iDartFont.VERASANS_8));
        tabbedGroup.setBackground(ResourceUtils
                .getColor(iDartColor.WIDGET_BACKGROUND));
        this.groupTabs = createTabs();

        for (IPatientTab tab : groupTabs) {
            tab.create();
            tab.getTabItem().getControl().setBackground(
                    ResourceUtils.getColor(iDartColor.WIDGET_BACKGROUND));
        }

    }

    private IPatientTab[] createTabs() {
        IPatientTab addressTab = new AddressTab();
        addressTab.setParent(tabbedGroup);
        addressTab.setStyle(SWT.NONE);
        addressTab.setSession(getHSession());

        IPatientTab treatmentManagementTab = new TreatmentManagementTab();
        treatmentManagementTab.setParent(tabbedGroup);
        treatmentManagementTab.setStyle(SWT.NONE);
        treatmentManagementTab.setSession(getHSession());

        IPatientTab clinicInfoTab = new ClinicInfoTab();
        clinicInfoTab.setParent(tabbedGroup);
        clinicInfoTab.setStyle(SWT.NONE);
        clinicInfoTab.setSession(getHSession());

        IPatientTab treatmentHistoryTab = new TreatmentHistoryTab();
        treatmentHistoryTab.setParent(tabbedGroup);
        treatmentHistoryTab.setStyle(SWT.NONE);
        treatmentHistoryTab.setSession(getHSession());

        return new IPatientTab[]{addressTab, treatmentManagementTab,
                clinicInfoTab, treatmentHistoryTab};
    }

    private void cmdEkapaSearchWidgetSelected() {
        SearchPatientGui ps = new SearchPatientGui(getHSession(), getShell(),
                true);

        Patient p = ps.getPatient();
        // check our local database if this patient already exists
        if (p != null) {
            Patient patient = PatientManager.getPatient(getHSession(), p
                    .getId());
            if (patient == null) {
                patient = p;

                MessageBox mSave = new MessageBox(getShell(), SWT.ICON_QUESTION
                        | SWT.YES | SWT.NO);
                mSave.setText(Messages.getString("patient.warning.ekapa.import.title")); //$NON-NLS-1$
                mSave.setMessage(MessageFormat.format(Messages.getString("patient.warning.ekapa.import"), //$NON-NLS-1$
                        patient.getPatientId(), patient.getLastname(), patient.getFirstNames()));
                switch (mSave.open()) {
                    case SWT.YES:
                        break;
                    case SWT.NO:
                        return;
                }

            }
            isAddnotUpdate = true;
            localPatient = patient;

            updateGUIforNewLocalPatient();
        }
        // if we've returned from the search GUI with the user having
        // pressed "cancel", enable the search button
        else if (!btnSearch.isDisposed() & !btnEkapaSearch.isDisposed()) {
            btnSearch.setEnabled(true);
            btnEkapaSearch.setEnabled(true);
        }
    }

    @Override
    protected void cmdSaveWidgetSelected() {
        // if we're updating a patient, 1st check that there
        // are actual changes to update


        if (!isSaveRequired()) {

            MessageBox mb = new MessageBox(getShell());
            mb.setText(Messages.getString("patient.info.noDbUpdateRequired.title")); //$NON-NLS-1$
            mb.setMessage(Messages.getString("patient.info.noDbUpdateRequired")); //$NON-NLS-1$
            mb.open();

        } else if (doSave()) {
            cmdCancelWidgetSelected();
        }
    }

    /**
     * Performs save operation if required.
     *
     * @return boolean true if save operation performed successfully or no save
     * is required.
     */
    private boolean doSave() {

        boolean checkOpenmrs = true;

        if (CentralizationProperties.centralization.equalsIgnoreCase("off"))
            checkOpenmrs = true;
        else if (CentralizationProperties.pharmacy_type.equalsIgnoreCase("F")
                || CentralizationProperties.pharmacy_type.equalsIgnoreCase("P"))
            checkOpenmrs = false;

        if (!isSaveRequired())
            return true;

        if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade"))
            if (checkOpenmrs && isAddnotUpdate) {
                //Verificar se o NID existe no OpenMRS
                try {
                    if (getServerStatus(JdbcProperties.urlBase).contains("Red")) {
                        log.trace(new Date() + " :Servidor OpenMRS offline, verifique a conexão com OpenMRS ou contacte o administrador");
                        showMessage(MessageDialog.WARNING, "Servidor OpenMRS Offline", "Por favor, verifique a conexão com OpenMRS para efectuar esta operação ");
                        return false;
                    } else {
                        User currentUser = LocalObjects.getUser(HibernateUtil.getNewSession());

                        assert currentUser != null;
                        if (ApiAuthRest.loginOpenMRS(currentUser)) {

                            String openMrsResource = new RestClient().getOpenMRSResource("patient?q=" + StringUtils.replace(txtPatientId.getText().trim(), " ", "%20"));

                            if (openMrsResource.length() == 14) {
                                MessageBox mb = new MessageBox(getShell());
                                mb.setText("Informação não encontrada"); //$NON-NLS-1$
                                mb.setMessage("NID inserido não existe no OpenMRS"); //$NON-NLS-1$
                                mb.open();
                                txtPatientId.setFocus();
                                return false;
                            }
                        } else {
                            log.error("O Utilizador " + currentUser.getUsername() + " não se encontra no OpenMRS ou serviço rest no OpenMRS não está  em funcionamento.");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        //Check if the patient is on a study
        //yes - update patient details
        setLocalPatient();

        if (fieldsOk() && confirmSave()) {
            Session sess = HibernateUtil.getNewSession();
            Patient oldPatient = PatientManager.getPatient(sess, localPatient
                    .getId());
            if (iDartProperties.isCidaStudy) {
                if (oldPatient != null) { //New Patient
                    if (!oldPatient.getCellphone().equals(localPatient.getCellphone())
                            && StudyManager.patientEverOnStudy(HibernateUtil.getNewSession(), localPatient.getId()))
                        updateMobilisrContactDetails(oldPatient.getCellphone(), LocalObjects.pharmacy.getPharmacyName(),
                                String.valueOf(localPatient.getId()),
                                localPatient.getCellphone());
                }
            } else {
                if (oldPatient != null) {
                    if (!oldPatient.getPatientId().equalsIgnoreCase(localPatient.getPatientId())) {
                        // update Packagedruginfos : Unsubmitted  records m  to openmrs  due to patientid mismatch
                        List<PackageDrugInfo> pdiList = TemporaryRecordsManager.getOpenmrsUnsubmittedPackageDrugInfos(getHSession(), oldPatient);
                        if (!pdiList.isEmpty())
                            TemporaryRecordsManager.updateOpenmrsUnsubmittedPackageDrugInfos(getHSession(), pdiList, localPatient);
                    }

                    if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade")) {
                        if (!oldPatient.getUuidopenmrs().trim().isEmpty()) {
                            if (oldPatient.getUuidopenmrs().equalsIgnoreCase(localPatient.getUuidopenmrs()) || oldPatient.getPatientId().equalsIgnoreCase(localPatient.getPatientId())) {

                                List<SyncOpenmrsDispense> syncOpenmrsDispenseList = PrescriptionManager.getAllSyncOpenmrsDispenseReadyToSaveByUUID(getHSession(), oldPatient.getUuidopenmrs());

                                if (!syncOpenmrsDispenseList.isEmpty()) {
                                    for (SyncOpenmrsDispense stp : syncOpenmrsDispenseList) {
                                        stp.setNid(localPatient.getPatientId());
                                        stp.setUuid(localPatient.getUuidopenmrs());
                                        PrescriptionManager.setUpdatedPatientNidSyncOpenmrsPatienFila(getHSession(), stp);
                                    }
                                }

                                SyncTempPatient syncTempPatient = AdministrationManager.getSyncTempPatienByUuid(getHSession(), oldPatient.getUuidopenmrs());

                                if(syncTempPatient != null) {
                                    syncTempPatient.setPatientid(localPatient.getPatientId());
                                    syncTempPatient.setUuid(localPatient.getUuidopenmrs());
                                    syncTempPatient.setCellphone(localPatient.getCellphone());
                                    syncTempPatient.setDatainiciotarv(localPatient.getAttributeByName("ARV Start Date").getValue());
                                    syncTempPatient.setFirstnames(localPatient.getFirstNames());
                                    syncTempPatient.setLastname(localPatient.getLastname());
                                    syncTempPatient.setAddress3(localPatient.getAddress3());
                                    syncTempPatient.setAddress2(localPatient.getAddress2());
                                    syncTempPatient.setAddress1(localPatient.getAddress1());
                                    syncTempPatient.setDateofbirth(localPatient.getDateOfBirth());
                                    syncTempPatient.setSex(localPatient.getSex());
                                    syncTempPatient.setSyncstatus('U');
                                    AdministrationManager.saveSyncTempPatient(getHSession(), syncTempPatient);
                                }

                                List<SyncTempDispense> syncTempDispensesList = AdministrationManager.getAllSyncTempDispenseByuuid(getHSession(), oldPatient.getUuidopenmrs());

                                if(!syncTempDispensesList.isEmpty()){
                                    for( SyncTempDispense std: syncTempDispensesList){
                                        std.setUuidopenmrs(localPatient.getUuidopenmrs());
                                        std.setPatientid(localPatient.getPatientId());
                                        AdministrationManager.saveSyncTempDispense(getHSession(),std);
                                    }
                                }

                            }
                        } else {
                            MessageBox msgbox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK | SWT.PRIMARY_MODAL);
                            msgbox.setText(Messages.getString("Paciente sem uuid")); //$NON-NLS-1$
                            msgbox.setMessage(Messages.getString("Paciente sem uuid, por favor actualize os dados do Paciente")); //$NON-NLS-1$
                            msgbox.open();
                        }
                    }

//                    if (!cmbEpisodeStartReason.getText().contains("nsito") && !cmbEpisodeStartReason.getText().contains("nidade")) {
//
//                        if (!oldPatient.getUuidopenmrs().equalsIgnoreCase(localPatient.getUuidopenmrs())) {
//                            // update Packagedruginfos : Unsubmitted  records m  to openmrs  due to patientid mismatch
//                            if (!syncOpenmrsDispenseList.isEmpty())
//                                for (SyncOpenmrsDispense stp : syncOpenmrsDispenseList) {
//                                    stp.setUuid(localPatient.getUuidopenmrs());
//                                    PrescriptionManager.setUUIDSyncOpenmrsPatienFila(getHSession(), stp);
//                                }
//                        }
//                    }
                }
            }
            return submitForm();
        } else {
            // if validation fails or the user chooses not to save, replace
            // the current localPatient, which
            // has had changes made to, with a fresh copy from the
            // database i.e. throw away the old copy.
            reloadPatient(true);
            return false;
        }
    }

    /**
     * @param preserveChanges
     */
    private void reloadPatient(boolean preserveChanges) {
        Set<PatientIdentifier> oldIdentifiers = localPatient.getPatientIdentifiers();

        getHSession().close();
        Session sess = HibernateUtil.getNewSession();
        Patient oldPatient = PatientManager.getPatient(sess, localPatient
                .getId());
        if (oldPatient != null) {
            localPatient = oldPatient;
        } else {
            localPatient = new Patient();
        }

        if (preserveChanges && identifierChangesMade) {
            localPatient.setPatientIdentifiers(oldIdentifiers);
            localPatient.setPatientId(localPatient.getPreferredIdentifier().getValue());
        }
        setHSession(sess);
        for (IPatientTab tab : groupTabs) {
            tab.setSession(getHSession());
        }
        if (epViewer != null) {
            epViewer.setSession(getHSession());
        }
        if (!preserveChanges)
            updateGUIforNewLocalPatient();
    }

    private void updateMobilisrContactDetails(String oldCellNo, String firstName, String lastName, String newCellNo) {
        try {
            if (StudyManager.patientEverOnStudy(getHSession(), localPatient.getId())) {
                MobilisrManager.updateMobilisrCellNo(oldCellNo, firstName, lastName, newCellNo);
            }
        } catch (RestCommandException e) {
            showMessage(MessageDialog.ERROR, "Error", "Error updating patients mobile number" +
                    " in Communicate");
        }
    }

    private boolean isSaveRequired() {
        return (!isAddnotUpdate && changesMadeToPatient()) || isAddnotUpdate;
    }

    @Override
    protected void setLogger() {
        setLog(Logger.getLogger(this.getClass()));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.celllife.idart.gui.misc.iDARTChangeListener#changed(java.lang.Object)
     */
    @Override
    public void changed(Object o) {
        // change occured in EpisodeViewer
        if (o instanceof Episode) {
            loadEpisodeDetails();
        }
    }

    public Map<String, String> arvStartDateValidation() {

        Map<String, String> map = new HashMap<String, String>();
        map.put(KEY_RESULT, String.valueOf(true));
        map.put(KEY_TITLE, EMPTY);
        map.put(KEY_MESSAGE, EMPTY);

        boolean newpatient = false;

        try {

            // Is this a new patient or a patient update?
            newpatient = (localPatient.getId() <= 0);

            // Checking if ARV Start date is before patient's date of birth.
            Date dteA = localPatient.getDateOfBirth();
            Date dteB = btnARVStart.getDate();
            if (dteA.after(dteB)) {
                String msg = Messages.getString("patient.error.arvStartDateBeforeDOB"); //$NON-NLS-1$
                map.put(KEY_RESULT, String.valueOf(false));
                map.put(KEY_TITLE, Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                map.put(KEY_MESSAGE, msg);
                getLog().info("ARV Test Failure: Patient birth date is after the ARV Start Date." + //$NON-NLS-1$
                        " ARV Start Date should be after the patient's date of birth"); //$NON-NLS-1$
                return map;
            }

            // If this is not a new patient, has he/she received ARV Drugs?
            boolean receivedARVDrug = false;
            List<Packages> packList = PackageManager.getAllPackagesForPatient(
                    getHSession(), localPatient);
            if (packList != null) {
                receivedARVDrug = PackageManager
                        .packagesContainARVDrug(packList);

                // Test only if patient received arv drugs
                // else issue error for not having arv drugs.
                if (receivedARVDrug) {
                    for (Packages packs : packList) {
                        List<PackagedDrugs> packDrugsList = packs
                                .getPackagedDrugs();
                        for (PackagedDrugs pd : packDrugsList) {
                            if (pd.getStock().getDrug().getSideTreatment() == 'F') {
                                Date dte_1 = btnARVStart.getDate();

                                Date dte_2 = packs.getPickupDate() == null ? new Date()
                                        : packs.getPickupDate();
                                if (dte_1.after(dte_2)) {
                                    // This means that a ARV Start date is after
                                    // the
                                    // pickup date for the pack.

                                    String msg = MessageFormat.format(Messages.getString("patient.error.arvStartDateDifferentToDispensedDate"), //$NON-NLS-1$
                                            iDARTUtil.format(packs.getPickupDate()));

                                    map.put(KEY_RESULT, String.valueOf(false));
                                    map.put(KEY_TITLE,
                                            Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                                    map.put(KEY_MESSAGE, msg);
                                    getLog().info(
                                            "ARV Start Date test failed: ARV Start Date [AFTER] pick up date for ARV package "  //$NON-NLS-1$
                                                    + pd.getParentPackage().getPackageId());
                                    return map;
                                }
                            }
                        }
                    }
                    getLog().info("ARV Start Date NOT before any ARV Packages."); //$NON-NLS-1$
                } else if (!newpatient && !receivedARVDrug) {
                    // Cannot test ARV Start Date on update
                    // if this patient has not been issued with
                    // any arv drug.
                    getLog().info(
                            "No ARV Drugs dispensed for patient " //$NON-NLS-1$
                                    + localPatient.getFirstNames());
                }
            }

            // If this is a new patient, test:
            // date < episode date if "new patient episode" :
            // new patient cannot have arv date before new episode.
            // date > episode date if "any other episodes" :
            // any other episode as new patient should have arv date before
            // episode start - *** if he has been receiving arv drugs,
            // but only the user knows this so we assume he has.

            Episode epi = localPatient.getEpisodeList().get(0);
            String episodeStartReason = epi.getStartReason();
            Date dteEpiStartDate = epi.getStartDate();
            Date dte_1 = btnARVStart.getDate();
            Date dte_2 = dteEpiStartDate;
            if (episodeStartReason.equalsIgnoreCase(Episode.REASON_NEW_PATIENT)) { //$NON-NLS-1$
                if (dte_1.before(dte_2)) {
                    String msg = MessageFormat.format(Messages.getString("patient.error.arvStartDateBeforeEpisodeStartDate"), //$NON-NLS-1$
                            iDARTUtil.format(dteEpiStartDate));
                    map.put(KEY_RESULT, String.valueOf(false));
                    map.put(KEY_TITLE, Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                    map.put(KEY_MESSAGE, msg);
                    getLog().info("ARV Start Date test [FAILED]: ARV Start Date [BEFORE] New Episode Start Date."); //$NON-NLS-1$
                    return map;
                } else {
                    getLog().info("ARV Start Date is correct for New Patient Episode"); //$NON-NLS-1$
                    return map;
                }
            } else {
                if (dte_1.after(dte_2) || dte_1.equals(dte_2)) {
                    String msg = MessageFormat.format(Messages.getString("patient.error.arvStartDateAfterEpisodeStart"), //$NON-NLS-1$
                            episodeStartReason, iDARTUtil.format(dteEpiStartDate), iDARTUtil.format(dteEpiStartDate));
                    map.put(KEY_RESULT, String.valueOf(false));
                    map.put(KEY_TITLE, Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                    map.put(KEY_MESSAGE, msg);
                    getLog().info("ARV Start Date test [FAILED]: ARV Start Date [AFTER] " //$NON-NLS-1$
                            + episodeStartReason + " Episode Start Date."); //$NON-NLS-1$
                    return map;
                } else {
                    getLog().info("ARV Start Date is correct for " //$NON-NLS-1$
                            + episodeStartReason + " Episode"); //$NON-NLS-1$
                    return map;
                }
            }
        } catch (Exception e) {
            if (localPatient.getEpisodes() == null) {
                getLog().error(
                        "Patient has not been set with any episodes, " //$NON-NLS-1$
                                + "therefore ARV Start Date check is not possible for new patient.", //$NON-NLS-1$
                        e);
                map.put(KEY_RESULT, String.valueOf(false));
                map.put(KEY_TITLE, Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                map.put(KEY_MESSAGE, Messages.getString("patient.error.incorrectEpisode")); //$NON-NLS-1$
            } else if (btnARVStart.getDate() == null) {
                getLog().info("ARV Start Date not specified: ARV Start Date Test [not done].");//$NON-NLS-1$


                map.put(KEY_RESULT, String.valueOf(true));
                map.put(KEY_TITLE, EMPTY);
                map.put(KEY_MESSAGE, EMPTY);
            } else {
                if (!newpatient) {
                    getLog().error("Problem testing ARV Start Date.", e); //$NON-NLS-1$
                    map.put(KEY_RESULT, String.valueOf(false));
                    map.put(KEY_TITLE, Messages.getString("patient.error.arvStartDate.title")); //$NON-NLS-1$
                    map.put(KEY_MESSAGE, Messages.getString("patient.error.arvStartDateTestFailed")); //$NON-NLS-1$
                }
            }
            return map;
        }
    }
}
