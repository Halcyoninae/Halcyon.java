package com.jackmeng.halcyon;

import javax.swing.*;

import com.jackmeng.halcyon.apps.impl_Identifiable;

import java.util.Optional;

public abstract interface impl_App
    extends
    Runnable,
    impl_Identifiable
{
  public ImageIcon icon();

  /**
   * Tooltip referencia
   *
   * @return
   */
  public String toolTip();

  public Optional< ImageIcon > rolloverIcon();

  public default boolean hidden()
  {
    return true;
  }
}