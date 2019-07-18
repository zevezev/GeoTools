function [mn,dy] = jd2md(yr,jd)

islp = isleapyear(yr);
dipm = [0 31 59 90 120 151 181 212 243 273 304 334 365];
dipm(3:end) = dipm(3:end)+islp;

mn = find(jd == dipm)-1;

if isempty(mn)
    mn = find(jd>dipm,1,'last');
    dy = jd-dipm(mn);
else
    dy = dipm(mn+1)-dipm(mn);
end
%--------------------------------------------------------------------------
function t = isleapyear(year)
%ISLEAPYEAR True for leap years.
%
%   ISLEAPYEAR(YEAR) returns 1's for the elements of YEAR that are leap
%   years and 0's for those that are not.  If YEAR is omitted, the current
%   year is used.  Gregorian calendar is assumed.
%
%   A year is a leap year if the following returns true
%
%       ( ~rem(year, 4) & rem(year, 100) ) | ~rem(year, 400)
%
%   A year is not a leap year if the following returns true
%
%      rem(year, 4) | ( ~rem(year, 100) & rem(year, 400) )

%   Author:      Peter J. Acklam
%   Time-stamp:  2002-03-03 12:51:45 +0100
%   E-mail:      pjacklam@online.no
%   URL:         http://home.online.no/~pjacklam

   narginchk(0, 1);

   if nargin == 0               % If no input argument...
      clk = clock;              % ...get current date and time...
      year = clk(1);            % ...and extract year.
   end

   t = ( ~rem(year, 4) & rem(year, 100) ) | ~rem(year, 400);