unit FrameUnit;

interface

uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, DataGridFrameUnit, StepinDataGridUnit, RzPanel, RzButton,
  ExtCtrls;

type
  TDataGridFrame1 = class(TDataGridFrame)
    RzToolbar1: TRzToolbar;
    RzToolbar5: TRzToolbar;
    RzSpacer4: TRzSpacer;
    btnCalculation: TRzBitBtn;
    RzPanel1: TRzPanel;
    grid: TStepinDataGrid;
  private
    { Private declarations }
  public
    { Public declarations }
  end;

var
  DataGridFrame1: TDataGridFrame1;

implementation

{$R *.dfm}

end.
